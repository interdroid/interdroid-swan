package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;
import interdroid.contextdroid.contextservice.SensorManager;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class which represents an Expression in ContextDroid.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class Expression implements Parcelable, Serializable,
Comparable<Expression> {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(Expression.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 1056115630721395151L;

	/**
	 * True if this is a leaf node.
	 */
	private boolean mLeafNode;

	/**
	 * Time until we need to reevaluate this expression.
	 */
	private long mDeferUntil;

	/**
	 * The left expression if this is not a leaf expression.
	 */
	private Expression mLeftExpression;
	/**
	 * The right expression if this is not a leaf expression.
	 */
	private Expression mRightExpression;

	/**
	 * The operator if this is a leaf expression.
	 */
	private String mOperator;

	/**
	 * The left value if this is a leaf expression.
	 */
	private TypedValue mLeftValue;

	/**
	 * The right value if this is a leaf expression.
	 */
	private TypedValue mRightValue;

	/**
	 * The comparator being used if this is not a leaf.
	 */
	private ComparatorStrategy mComparator;

	/**
	 * The result of the expression.
	 */
	private int mResult = ContextManager.UNDEFINED;

	/**
	 * The id for this expression.
	 */
	private String mId;

	/**
	 * Constructs an expression from a parcel.
	 * @param in the parcel to read from.
	 */
	private Expression(final Parcel in) {
		readFromParcel(in);
	}

	/**
	 * Constructs a non leaf node expression.
	 * @param left the left expression.
	 * @param operator the joining operator.
	 * @param right the right expression.
	 */
	public Expression(final Expression left, final String operator,
			final Expression right) {
		this.mLeafNode = false;
		this.mLeftExpression = left;
		this.mRightExpression = right;
		this.mOperator = operator;
	}

	/**
	 * Constructs a non leaf node expression with no right expression.
	 * @param operator the operator.
	 * @param expression the left expression.
	 */
	public Expression(final String operator, final Expression expression) {
		this(expression, operator, null);
	}

	/**
	 * Constructs a leaf expression.
	 * @param left the left value
	 * @param comparator the comparator, possibly including strategy
	 * @param right the right value
	 */
	public Expression(final TypedValue left, final String comparator,
			final TypedValue right) {
		this.mLeafNode = true;
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = ComparatorStrategy.parse(comparator);

	}

	/**
	 * @return the ID for this expression.
	 */
	public final String getId() {
		return mId;
	}

	/**
	 * Sets the ID for this expression.
	 * @param id the id to set to
	 */
	public final void setId(final String id) {
		this.mId = id;
	}

	/**
	 * Initializes this expression tree with the sensor manager.
	 * @param id the id of the expression
	 * @param sensorManager the sensor manager to initialize with
	 * @throws SensorConfigurationException
	 * 	if the sensor does not accept the configuration for the expression.
	 * @throws SensorSetupFailedException if initializing fails.
	 */
	public final void initialize(final String id,
			final SensorManager sensorManager)
					throws SensorConfigurationException,
					SensorSetupFailedException {
		this.mId = id;
		if (mLeafNode) {
			mLeftValue.initialize(id + ".L", sensorManager);
			if (mRightValue != null) {
				mRightValue.initialize(id + ".R", sensorManager);
			}
		} else {
			mLeftExpression.initialize(id + ".L", sensorManager);
			if (mRightExpression != null) {
				mRightExpression.initialize(id + ".R", sensorManager);
			}
		}
	}

	/**
	 * Destroys this expression with the sensor manager.
	 * @param id the id for this expression.
	 * @param sensorManager the sensor manager to destroy with
	 * @throws ContextDroidException if something goes wrong.
	 */
	public final void destroy(final String id,
			final SensorManager sensorManager)
					throws ContextDroidException {
		if (mLeafNode) {
			mLeftValue.destroy(id + ".L", sensorManager);
			if (mRightValue != null) {
				mRightValue.destroy(id + ".R", sensorManager);
			}
		} else {
			mLeftExpression.destroy(id + ".L", sensorManager);
			if (mRightExpression != null) {
				mRightExpression.destroy(id + ".R", sensorManager);
			}
		}
	}

	/**
	 * @return the result of the expression.
	 * @throws ContextDroidException if something goes wrong.
	 */
	public final boolean evaluate() throws ContextDroidException {
		int previousResult = mResult;
		evaluate(System.currentTimeMillis());
		return previousResult != mResult;
	}

	/**
	 * Evaluates this expression given the requested time.
	 * @param now the epoch time to evaluate the expression at in milliseconds
	 * @throws ContextDroidException if something goes wrong.
	 */
	// TODO: shortcircuit some non leaf node evaluations
	// left = TRUE, comparator = &&
	public final void evaluate(final long now) throws ContextDroidException {
		if (now < mDeferUntil) {
			LOG.debug("deffered until: {} not evaluating", mDeferUntil);
			return;
		}
		if (mLeafNode) {
			try {
				evaluateLeafNode(now);
			} catch (NoValuesInIntervalException e) {
				setResult(ContextManager.UNDEFINED);
			}
		} else {
			mLeftExpression.evaluate(now);
			if (mRightExpression != null) {
				mRightExpression.evaluate(now);
				mDeferUntil = Math.min(mLeftExpression.mDeferUntil,
						mRightExpression.mDeferUntil);
			} else {
				mDeferUntil = mLeftExpression.mDeferUntil;
			}
			evaluateNonLeafNode();
		}
	}

	/**
	 * Evaluates a non leaf node.
	 */
	private void evaluateNonLeafNode() {
		int leftResult = mLeftExpression.getResult();
		int rightResult = ContextManager.UNDEFINED;
		if (mRightExpression != null) {
			mRightExpression.getResult();
		}
		if ("!".equals(mOperator)) {
			if (leftResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE) {
				setResult(ContextManager.FALSE);
			} else if (leftResult == ContextManager.FALSE) {
				setResult(ContextManager.TRUE);
			}
		} else if ("&&".equals(mOperator)) {
			if (leftResult == ContextManager.UNDEFINED
					|| rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE
					&& rightResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE);
			} else {
				setResult(ContextManager.FALSE);
			}
		} else if ("||".equals(mOperator)) {
			if (leftResult == ContextManager.UNDEFINED
					&& rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED);
			} else if (leftResult == ContextManager.TRUE
					|| rightResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE);
			} else {
				setResult(ContextManager.FALSE);
			}
		}
	}

	/**
	 * Evaluates a leaf node.
	 * @param now the time to evaluate at
	 * @throws ContextDroidException if something goes wrong.
	 */
	private void evaluateLeafNode(final long now) throws ContextDroidException {
		TimestampedValue[] left = mLeftValue.getValues(mId + ".L", now);
		TimestampedValue[] right = mRightValue.getValues(mId + ".R", now);

		int endResult;
		if (mComparator.getStrategy().equals(Strategy.ALL)) {
			endResult = ContextManager.TRUE;
		} else {
			endResult = ContextManager.FALSE;
		}

		for (TimestampedValue leftItem : left) {
			for (TimestampedValue rightItem : right) {
				int tempResult = evaluateLeafItem(leftItem.getValue(),
						rightItem.getValue());
				if (mComparator.getStrategy()
						.equals(Strategy.ALL)) {
					if (tempResult == ContextManager.FALSE) {
						setResult(now, ContextManager.FALSE, left, right);
						return;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				} else {
					if (tempResult == ContextManager.TRUE) {
						setResult(now, ContextManager.TRUE, left, right);
						return;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				}

			}
		}
		setResult(now, endResult, left, right);
	}

	/**
	 * Sets the result for the given expression.
	 * @param now the time that was evaluated
	 * @param newResult the result of the evaluation
	 * @param left the left values
	 * @param right the right values
	 */
	private void setResult(final long now, final int newResult,
			final Object[] left, final Object[] right) {
		if (newResult == ContextManager.UNDEFINED) {
			setResult(newResult);
			return;
		}
		// TODO: cache these values?
		if (!mLeftValue.hasCurrentTime() && !mRightValue.hasCurrentTime()) {
			mDeferUntil = Math.min(mLeftValue.deferUntil(),
					mRightValue.deferUntil());
			setResult(newResult);
			return;
		}

		// TODO: think about when left or right is an array
		long leftTime = (Long) ((TimestampedValue) left[0]).getValue();
		long rightTime = (Long) ((TimestampedValue) right[0]).getValue();

		switch (mComparator.getComparator()) {
		case LESS_THAN:
		case LESS_THAN_OR_EQUALS:
			if (mLeftValue.hasCurrentTime()) {
				if (newResult == ContextManager.TRUE) {
					// set defer until to right - left
					mDeferUntil = now + rightTime - leftTime;
				} else {
					// it's FALSE and won't be TRUE anymore
					mDeferUntil = Long.MAX_VALUE;
				}
			} else {
				// right has current time
				if (newResult == ContextManager.TRUE) {
					// it's TRUE and won't be FALSE anymore
					mDeferUntil = Long.MAX_VALUE;
				} else {
					// set defer until to left - right
					mDeferUntil = now + leftTime - rightTime;
				}
			}
			break;
		case GREATER_THAN:
		case GREATER_THAN_OR_EQUALS:
			if (mLeftValue.hasCurrentTime()) {
				if (newResult == ContextManager.TRUE) {
					// it's TRUE and won't be FALSE anymore
					mDeferUntil = Long.MAX_VALUE;
				} else {
					// set defer until to right - left
					mDeferUntil = now + rightTime - leftTime;
				}
			} else {
				if (newResult == ContextManager.TRUE) {
					// set defer until to left - right
					mDeferUntil = now + leftTime - rightTime;
				} else {
					// it's FALSE and won't be TRUE anymore
					mDeferUntil = Long.MAX_VALUE;
				}
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Evaluates a leaf item performing the comparison.
	 * @param left the left side values
	 * @param right the right side values
	 * @return ContextManager.FALSE or ContextManager.TRUE
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int evaluateLeafItem(final Object left, final Object right) {
		int result = ContextManager.FALSE;
		switch (mComparator.getComparator()) {
		case LESS_THAN:
			if (((Comparable) left).compareTo(right) < 0) {
				result = ContextManager.TRUE;
			}
			break;
		case LESS_THAN_OR_EQUALS:
			if (((Comparable) left).compareTo(right) <= 0) {
				result = ContextManager.TRUE;
			}
			break;
		case GREATER_THAN:
			if (((Comparable) left).compareTo(right) > 0) {
				result = ContextManager.TRUE;
			}
			break;
		case GREATER_THAN_OR_EQUALS:
			if (((Comparable) left).compareTo(right) >= 0) {
				result = ContextManager.TRUE;
			}
			break;
		case EQUALS:
			if (((Comparable) left).compareTo(right) == 0) {
				result = ContextManager.TRUE;
			}
			break;
		case EQUALS_NOT:
			if (((Comparable) left).compareTo(right) != 0) {
				result = ContextManager.TRUE;
			}
			break;
		case REGEX_MATCH:
			if (((String) left).matches((String) right)) {
				result = ContextManager.TRUE;
			}
			break;
		case STRING_CONTAINS:
			if (((String) left).contains((String) right)) {
				result = ContextManager.TRUE;
			}
			break;
		default:
			throw new AssertionError("Unknown comparator '" + mComparator
					+ "'. Should not happen");
		}
		return result;
	}

	/**
	 * Sets the result of the last evaluation.
	 * @param newResult the result to set to.
	 */
	protected final void setResult(final int newResult) {
		mResult = newResult;
	}

	/**
	 * @return the result of the last evaluation.
	 */
	public final int getResult() {
		return mResult;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		if (mLeafNode) {
			dest.writeInt(1);
		} else {
			dest.writeInt(0);
		}
		if (mLeafNode) {
			dest.writeParcelable(mLeftValue, 0);
			dest.writeParcelable(mRightValue, 0);
			dest.writeInt(mComparator.convert());
		} else {
			dest.writeParcelable(mLeftExpression, 0);
			dest.writeParcelable(mRightExpression, 0);
			dest.writeString(mOperator);
		}
	}

	/**
	 * Read from parcel.
	 *
	 * @param in
	 *            the in
	 */
	private void readFromParcel(final Parcel in) {
		mLeafNode = (in.readInt() == 1);
		if (mLeafNode) {
			mLeftValue = in.readParcelable(this.getClass().getClassLoader());
			mRightValue = in.readParcelable(this.getClass().getClassLoader());
			mComparator = ComparatorStrategy.convert(in.readInt());
		} else {
			mLeftExpression = in
					.readParcelable(this.getClass().getClassLoader());
			mRightExpression = in.readParcelable(this.getClass()
					.getClassLoader());
			mOperator = in.readString();
		}
	}

	/** The CREATOR. */
	public static final Expression.Creator<Expression> CREATOR =
			new Expression.Creator<Expression>() {

		@Override
		public Expression createFromParcel(final Parcel source) {
			return new Expression(source);
		}

		@Override
		public Expression[] newArray(final int size) {
			return new Expression[size];
		}
	};

	/**
	 * Sets the next time this expression should be evaluated.
	 * @param evaluateAt the time to evalue next at
	 */
	public final void setNextEvaluationTime(final long evaluateAt) {
		mDeferUntil = evaluateAt;
	}

	/**
	 * @return the next time this expression should be evaluated
	 */
	public final long getNextEvaluationTime() {
		return mDeferUntil;
	}

	@Override
	public final int compareTo(final Expression another) {
		long difference = getNextEvaluationTime()
				- another.getNextEvaluationTime();
		if (difference == 0) {
			return 0;
		} else if (difference < 0) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
	public final String toString() {
		if (mId != null) {
			return mId;
		}
		if (mLeafNode) {
			return mLeftValue + " " + mComparator + " " + mRightValue;
		} else {
			if (mRightExpression == null) {
				return mOperator + "(" + mLeftExpression + ")";
			} else {
				return "(" + mLeftExpression + " " + mOperator + " "
						+ mRightExpression + ")";
			}
		}
	}

	/**
	 * @param left true if the side desired is the left side
	 * @return the typedValue side requested or null
	 */
	public final TypedValue getTypedValue(final boolean left) {
		if (left) {
			return mLeftValue;
		} else {
			return mRightValue;
		}
	}

	/**
	 * @return the comparison strategy for this expression.
	 */
	public final ComparatorStrategy getComparator() {
		return mComparator;
	}

}
