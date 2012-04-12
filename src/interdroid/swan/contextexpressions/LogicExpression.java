package interdroid.swan.contextexpressions;

import android.os.Parcel;
import android.os.Parcelable;
import interdroid.swan.SwanException;
import interdroid.swan.ContextManager;
import interdroid.swan.contextservice.SensorConfigurationException;
import interdroid.swan.contextservice.SensorManager;
import interdroid.swan.contextservice.SensorSetupFailedException;

/**
 * An expression which combines two sub-expressions with an operator.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class LogicExpression extends Expression {
	/**
	 *
	 */
	private static final long serialVersionUID = 6206620564361837288L;
	/**
	 * The left expression.
	 */
	private final Expression mLeftExpression;
	/**
	 * The right expression.
	 */
	private final Expression mRightExpression;

	/**
	 * The comparator being used to combine left and right.
	 */
	private final LogicOperator mOperator;

	/**
	 * Constructs am operator expression.
	 * 
	 * @param left
	 *            the left expression.
	 * @param operator
	 *            the joining operator.
	 * @param right
	 *            the right expression.
	 */
	public LogicExpression(final Expression left, final LogicOperator operator,
			final Expression right) {
		// TODO: Verify operator matches arguments properly
		this.mLeftExpression = left;
		this.mRightExpression = right;
		this.mOperator = operator;
	}

	/**
	 * Constructs an operator expression with no right expression.
	 * 
	 * @param operator
	 *            the operator. Must be LogicOperator.NOT.
	 * @param expression
	 *            the left expression.
	 */
	public LogicExpression(final LogicOperator operator,
			final Expression expression) {
		this(expression, operator, null);

		if (operator != LogicOperator.NOT) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Construct from a parcel.
	 * 
	 * @param in
	 *            the parcel to read values from.
	 */
	protected LogicExpression(final Parcel in) {
		super(in);
		mLeftExpression = in.readParcelable(getClass().getClassLoader());
		mOperator = LogicOperator.convert(in.readInt());
		mRightExpression = in.readParcelable(getClass().getClassLoader());
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<LogicExpression> CREATOR = new Parcelable.Creator<LogicExpression>() {
		@Override
		public LogicExpression createFromParcel(final Parcel in) {
			return new LogicExpression(in);
		}

		@Override
		public LogicExpression[] newArray(final int size) {
			return new LogicExpression[size];
		}
	};

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		setId(id);
		mLeftExpression.initialize(id + ".L", sensorManager);
		if (mRightExpression != null) {
			mRightExpression.initialize(id + ".R", sensorManager);
		}
	}

	@Override
	public final void destroy(final String id, final SensorManager sensorManager)
			throws SwanException {
		mLeftExpression.destroy(id + ".L", sensorManager);
		if (mRightExpression != null) {
			mRightExpression.destroy(id + ".R", sensorManager);
		}
	}

	private boolean leftFirst() {
		// if we can defer one of the expressions for a longer time than we need
		// to keep history for the other, we might be able to turn off a sensor
		// for a while (e.g. one.deferUntil - other.history) iff we're able to
		// short circuit the expression. That depends one the operator and the
		// value of the first expression

		long leftDeferUntil = mLeftExpression.getDeferUntil();
		long leftHistory = mLeftExpression.getHistoryLength();
		long rightDeferUntil = mRightExpression.getDeferUntil();
		long rightHistory = mRightExpression.getHistoryLength();
		return (leftDeferUntil - rightHistory > rightDeferUntil - leftHistory);
	}

	@Override
	protected final void evaluateImpl(final long now)
			throws SwanException {

		boolean leftFirst = leftFirst();

		Expression firstExpression = leftFirst ? mLeftExpression
				: mRightExpression;
		Expression lastExpression = leftFirst ? mRightExpression
				: mLeftExpression;

		firstExpression.evaluate(now);
		int firstResult = firstExpression.getResult();

		// Can we short circuit and don't evaluate the last expression?
		// FALSE && ?? -> FALSE
		// TRUE || ?? -> TRUE

		if (firstResult == ContextManager.FALSE
				&& mOperator.equals(LogicOperator.AND)) {
			setResult(ContextManager.FALSE, now);
			// we can now turn off the last expression for a while
			lastExpression.sleepAndBeReadyAt(firstExpression.getDeferUntil());
			return;
		} else if ((firstResult == ContextManager.TRUE)
				&& mOperator.equals(LogicOperator.OR)) {
			setResult(ContextManager.TRUE, now);
			// we can now turn off the last expression for a while
			lastExpression.sleepAndBeReadyAt(firstExpression.getDeferUntil());
			return;
		}

		// We might evaluate the right side. Test for null in case we deal with
		// a unary logic expresson
		int lastResult = ContextManager.UNDEFINED;
		if (lastExpression != null) {
			lastExpression.evaluate(now);
			lastResult = lastExpression.getResult();
		}
		switch (mOperator) {
		case NOT:
			if (firstResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (firstResult == ContextManager.TRUE) {
				setResult(ContextManager.FALSE, now);
			} else if (firstResult == ContextManager.FALSE) {
				setResult(ContextManager.TRUE, now);
			}
			break;
		case AND:
			if (firstResult == ContextManager.UNDEFINED
					|| lastResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (firstResult == ContextManager.TRUE
					&& lastResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE, now);
			} else {
				setResult(ContextManager.FALSE, now);
			}
			break;
		case OR:
			if (firstResult == ContextManager.UNDEFINED
					&& lastResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (firstResult == ContextManager.TRUE
					|| lastResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE, now);
			} else {
				setResult(ContextManager.FALSE, now);
			}
			break;
		default:
			throw new IllegalArgumentException("Unknown Logical Operator: "
					+ mOperator);
		}
	}

	@Override
	protected final long getDeferUntilImpl() {
		long mDeferUntil;
		if (mRightExpression != null) {
			// Smart energy optimization
			// A | B | && | ||
			// -------------------
			// T | T | min | max
			// T | F | min | A
			// F | T | min | B
			// F | F | max | min

			// TODO: ContextManager.FALSE should be in a custom enum

			if (mOperator.equals(LogicOperator.AND)
					&& mLeftExpression.getResult() == ContextManager.FALSE
					&& mRightExpression.getResult() == ContextManager.FALSE) {
				mDeferUntil = Math.max(mLeftExpression.getDeferUntil(),
						mRightExpression.getDeferUntil());
			} else if (mOperator.equals(LogicOperator.OR)
					&& mLeftExpression.getResult() == ContextManager.TRUE
					&& mRightExpression.getResult() == ContextManager.TRUE) {
				mDeferUntil = Math.max(mLeftExpression.getDeferUntil(),
						mRightExpression.getDeferUntil());
			} else if (mOperator.equals(LogicOperator.OR)
					&& mLeftExpression.getResult() == ContextManager.TRUE) {
				mDeferUntil = mLeftExpression.getDeferUntil();
			} else if (mOperator.equals(LogicOperator.OR)
					&& mRightExpression.getResult() == ContextManager.TRUE) {
				mDeferUntil = mRightExpression.getDeferUntil();
			} else {
				mDeferUntil = Math.min(mLeftExpression.getDeferUntil(),
						mRightExpression.getDeferUntil());
			}
		} else {
			mDeferUntil = mLeftExpression.getDeferUntil();
		}
		return mDeferUntil;
	}

	@Override
	protected final String toStringImpl() {
		if (mRightExpression == null) {
			return mOperator + "(" + mLeftExpression + ")";
		} else {
			return "(" + mLeftExpression + " " + mOperator + " "
					+ mRightExpression + ")";
		}
	}

	@Override
	protected final void writeToParcelImpl(final Parcel dest, final int flags) {
		dest.writeParcelable(mLeftExpression, flags);
		dest.writeInt(mOperator.convert());
		dest.writeParcelable(mRightExpression, flags);
	}

	@Override
	protected final int getSubtypeId() {
		return LOGIC_EXPRESSION_TYPE;
	}

	@Override
	protected final String toParseStringImpl() {
		if (mRightExpression == null) {
			return mOperator.toString() + " " + mLeftExpression.toParseString();
		} else {
			return "(" + mLeftExpression.toParseString() + " " + mOperator
					+ " " + mRightExpression.toParseString() + ")";
		}
	}

	@Override
	public void sleepAndBeReadyAt(long readyTime) {
		mLeftExpression.sleepAndBeReadyAt(readyTime);
		mRightExpression.sleepAndBeReadyAt(readyTime);
	}

	@Override
	public long getHistoryLength() {
		return Math.max(mLeftExpression.getHistoryLength(),
				mRightExpression.getHistoryLength());
	}

	protected boolean hasCurrentTime() {
		return false;
	}

	@Override
	public TimestampedValue[] getValues(String string, long now) {
		return new TimestampedValue[] { new TimestampedValue(getResult(),
				getLastEvaluationTime()) };
	}

	/**
	 * @return the left side of this expression.
	 */
	public Expression getLeftExpression() {
		return mLeftExpression;
	}

	/**
	 * @return the right side of this expression.
	 */
	public Expression getRightExpression() {
		return mRightExpression;
	}

	/**
	 * @return the operator for this expression.
	 */
	public LogicOperator getOperator() {
		return mOperator;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return HistoryReductionMode.DEFAULT_MODE;
	}

	@Override
	public boolean isConstant() {
		// TODO Auto-generated method stub
		return false;
	}

}
