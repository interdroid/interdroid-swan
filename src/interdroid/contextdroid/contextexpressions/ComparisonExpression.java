package interdroid.contextdroid.contextexpressions;

import android.os.Parcel;
import android.os.Parcelable;
import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;

/**
 * An expression which compares two values.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ComparisonExpression extends Expression {
	/**
	 *
	 */
	private static final long	serialVersionUID	= 4074214646065165490L;

	/**
	 * The comparator used to compare the values.
	 */
	private final Comparator mComparator;

	/**
	 * The strategy used when comparing the values.
	 */
	private final Strategy mStrategy;

	/**
	 * The left value.
	 */
	private final Expression mLeftValue;

	/**
	 * The right value.
	 */
	private final Expression mRightValue;

	/**
	 * The time until this value expression needs to be evaluated again.
	 */
	private long mDeferUntil = -1;

	/**
	 * Constructs a comparison expression.
	 * @param left the left expression
	 * @param comparator the comparator
	 * @param strategy the strategy used when comparing.
	 * @param right the right expression
	 */
	public ComparisonExpression(final Expression left,
			final Comparator comparator, final Strategy strategy,
			final Expression right) {
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = comparator;
		this.mStrategy = strategy;
	}

	/**
	 * Construct from a parcel.
	 * @param in the parcel to read values from.
	 */
	protected ComparisonExpression(final Parcel in) {
		super(in);
		mLeftValue = in.readParcelable(ComparisonExpression.class.getClassLoader());
		mComparator = Comparator.convert(in.readInt());
		mStrategy = Strategy.convert(in.readInt());
		mRightValue = in.readParcelable(ComparisonExpression.class.getClassLoader());
	}

	/**
	 * Constructs a comparison expression.
	 * This is a convenience which wraps the TypedValues in
	 * TypedValueExpressions until we refactor TypedValue to be
	 * an expression directly.
	 * @param left the left expression
	 * @param comparator the comparator
	 * @param strategy the strategy used when comparing.
	 * @param right the right expression
	 */
	public ComparisonExpression(TypedValue leftValue, Comparator comparator,
			Strategy strategy, TypedValue rightValue) {
		mLeftValue = new TypedValueExpression(leftValue);
		mRightValue = new TypedValueExpression(rightValue);
		mStrategy = strategy;
		mComparator = comparator;
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<ComparisonExpression> CREATOR
	= new Parcelable.Creator<ComparisonExpression>() {
		@Override
		public ComparisonExpression createFromParcel(final Parcel in) {
			return new ComparisonExpression(in);
		}

		@Override
		public ComparisonExpression[] newArray(final int size) {
			return new ComparisonExpression[size];
		}
	};

	/**
	 * @param left true if the side desired is the left side
	 * @return the typedValue side requested or null
	 */
	protected final Expression getExpression(final boolean left) {
		if (left) {
			return mLeftValue;
		} else {
			return mRightValue;
		}
	}

	/**
	 * @return the comparison for this expression.
	 */
	protected final Comparator getComparator() {
		return mComparator;
	}

	/**
	 * @return the strategy for this expression.
	 */
	protected final Strategy getStrategy() {
		return mStrategy;
	}

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
					throws SensorConfigurationException,
					SensorSetupFailedException {
		setId(id);
		mLeftValue.initialize(id + ".L", sensorManager);
		mRightValue.initialize(id + ".R", sensorManager);
	}

	@Override
	public final void destroy(final String id,
			final SensorManager sensorManager)
					throws ContextDroidException {
		mLeftValue.destroy(id + ".L", sensorManager);
		mRightValue.destroy(id + ".R", sensorManager);
	}

	/**
	 * Evaluates this value expression.
	 * @param now the time to evaluate at
	 * @throws ContextDroidException if something goes wrong.
	 */
	@Override
	protected final void evaluateImpl(final long now)
			throws ContextDroidException {

		TimestampedValue[] left = mLeftValue.getValues(getId() + ".L", now);
		TimestampedValue[] right = mRightValue.getValues(getId() + ".R", now);

		int endResult;
		if (mStrategy.equals(Strategy.ALL)) {
			endResult = ContextManager.TRUE;
		} else {
			endResult = ContextManager.FALSE;
		}

		for (TimestampedValue leftItem : left) {
			for (TimestampedValue rightItem : right) {
				int tempResult = evaluateLeafItem(leftItem.getValue(),
						rightItem.getValue());
				if (mStrategy
						.equals(Strategy.ALL)) {
					if (tempResult == ContextManager.FALSE) {
						endResult = ContextManager.FALSE;
						break;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				} else {
					if (tempResult == ContextManager.TRUE) {
						endResult = ContextManager.TRUE;
						break;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				}

			}
		}
		setResult(endResult, now);

		// We can shortcut values of time.current if we know the values
		// so we pre-calculate and cache the result.
		calculateDeferUntil(now, left, right);
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
		switch (mComparator) {
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
		case NOT_EQUALS:
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
	 * Sets the result for the given expression.
	 * @param now the time that was evaluated
	 * @param left the left values
	 * @param right the right values
	 */
	private void calculateDeferUntil(final long now,
			final Object[] left, final Object[] right) {
		// TODO: cache these values?
		if (!mLeftValue.hasCurrentTime() && !mRightValue.hasCurrentTime()) {
			mDeferUntil = Math.min(mLeftValue.getDeferUntil(),
					mRightValue.getDeferUntil());
		} else {

			// TODO: think about when left or right is an array
			long leftTime = (Long) ((TimestampedValue) left[0]).getValue();
			long rightTime = (Long) ((TimestampedValue) right[0]).getValue();

			switch (mComparator) {
			case LESS_THAN:
			case LESS_THAN_OR_EQUALS:
				if (mLeftValue.hasCurrentTime()) {
					if (getResult() == ContextManager.TRUE) {
						// It switches when we pass right time
						mDeferUntil = rightTime;
					} else {
						// it's FALSE and won't be TRUE anymore
						mDeferUntil = Long.MAX_VALUE;
					}
				} else {
					// right has current time
					if (getResult() == ContextManager.FALSE) {
						// it's TRUE and won't be FALSE anymore
						mDeferUntil = Long.MAX_VALUE;
					} else {
						// It switches when we pass left time
						mDeferUntil = leftTime;
					}
				}
				break;
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUALS:
				if (mLeftValue.hasCurrentTime()) {
					if (getResult() == ContextManager.TRUE) {
						// it's TRUE and won't be FALSE anymore
						mDeferUntil = Long.MAX_VALUE;
					} else {
						// set defer until to right time
						mDeferUntil = rightTime;
					}
				} else {
					if (getResult() == ContextManager.FALSE) {
						// set defer until to left time
						mDeferUntil = leftTime;
					} else {
						// it's FALSE and won't be TRUE anymore
						mDeferUntil = Long.MAX_VALUE;
					}
				}
				break;
			case EQUALS:
				if (getResult() == ContextManager.TRUE) {
					// it's TRUE and will be FALSE in an instant
					mDeferUntil = now + 1;
				} else {
					// It switches when we hit the side without current time
					if (mLeftValue.hasCurrentTime()) {
						mDeferUntil = rightTime;
					} else {
						mDeferUntil = leftTime;
					}
				}
				break;
			case NOT_EQUALS:
				if (getResult() == ContextManager.FALSE) {
					// it's TRUE and will be FALSE in an instant
					mDeferUntil = now + 1;
				} else {
					// It switches when we hit the side without current time
					if (mLeftValue.hasCurrentTime()) {
						mDeferUntil = rightTime;
					} else {
						mDeferUntil = leftTime;
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Unknown time comparator: "
						+ mComparator);
			}
		}
	}

	@Override
	protected final String toStringImpl() {
		return mLeftValue + " " + mComparator + " " + mRightValue;
	}

	@Override
	protected final String toParseStringImpl() {
		return mLeftValue.toParseString()
				+ " " + mComparator.toParseString() + " "
				+ mRightValue.toParseString();
	}

	@Override
	protected final void writeToParcelImpl(final Parcel dest, final int flags) {
		dest.writeParcelable(mLeftValue, flags);
		dest.writeInt(mComparator.convert());
		dest.writeParcelable(mRightValue, flags);
		dest.writeLong(mDeferUntil);
	}

	@Override
	protected final long getDeferUntilImpl() {
		return mDeferUntil;
	}

	@Override
	protected final int getSubtypeId() {
		return VALUE_EXPRESSION_TYPE;
	}

	@Override
	protected boolean hasCurrentTime() {
		return false;
	}

	@Override
	public TimestampedValue[] getValues(String string, long now) {
		return new TimestampedValue[] {new TimestampedValue(getResult(),
				getLastEvaluationTime())};
	}

	/**
	 * @return the left side of this expression.
	 */
	public Expression getLeftExpression() {
		return mLeftValue;
	}

	/**
	 * @return the right side of this expression.
	 */
	public Expression getRightExpression() {
		return mRightValue;
	}

}
