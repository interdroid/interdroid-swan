package interdroid.contextdroid.contextexpressions;

import android.os.Parcel;
import android.os.Parcelable;
import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;

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
	private static final long	serialVersionUID	= 6206620564361837288L;
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
	 * @param left the left expression.
	 * @param operator the joining operator.
	 * @param right the right expression.
	 */
	public LogicExpression(final Expression left, final LogicOperator operator,
			final Expression right) {
		this.mLeftExpression = left;
		this.mRightExpression = right;
		this.mOperator = operator;
	}

	/**
	 * Constructs an operator expression with no right expression.
	 * @param operator the operator. Must be LogicOperator.NOT.
	 * @param expression the left expression.
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
	 * @param in the parcel to read values from.
	 */
	protected LogicExpression(final Parcel in) {
		super(in);
		ClassLoader loader = LogicExpression.class.getClassLoader();
		mLeftExpression = in.readParcelable(loader);
		mOperator = LogicOperator.convert(in.readInt());
		mRightExpression = in.readParcelable(loader);
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<LogicExpression> CREATOR
	= new Parcelable.Creator<LogicExpression>() {
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
					throws SensorConfigurationException,
					SensorSetupFailedException {
		setId(id);
		mLeftExpression.initialize(id + ".L", sensorManager);
		if (mRightExpression != null) {
			mRightExpression.initialize(id + ".R", sensorManager);
		}
	}

	@Override
	public final void destroy(final String id,
			final SensorManager sensorManager)
					throws ContextDroidException {
		mLeftExpression.destroy(id + ".L", sensorManager);
		if (mRightExpression != null) {
			mRightExpression.destroy(id + ".R", sensorManager);
		}
	}

	@Override
	protected final void evaluateImpl(final long now)
			throws ContextDroidException {
		// We always evaluate the left side.
		mLeftExpression.evaluate(now);
		int leftResult = mLeftExpression.getResult();

		// Can we short circuit the rest?
		if (leftResult == ContextManager.TRUE
				&& mOperator.equals(LogicOperator.AND)) {
			setResult(leftResult, now);
		}

		// We might evaluate the right side.
		int rightResult = ContextManager.UNDEFINED;
		if (mRightExpression != null) {
			mRightExpression.evaluate(now);
			rightResult = mRightExpression.getResult();
		}
		switch (mOperator) {
		case NOT:
			if (leftResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (leftResult == ContextManager.TRUE) {
				setResult(ContextManager.FALSE, now);
			} else if (leftResult == ContextManager.FALSE) {
				setResult(ContextManager.TRUE, now);
			}
			break;
		case AND:
			if (leftResult == ContextManager.UNDEFINED
			|| rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (leftResult == ContextManager.TRUE
					&& rightResult == ContextManager.TRUE) {
				setResult(ContextManager.TRUE, now);
			} else {
				setResult(ContextManager.FALSE, now);
			}
			break;
		case OR:
			if (leftResult == ContextManager.UNDEFINED
			&& rightResult == ContextManager.UNDEFINED) {
				setResult(ContextManager.UNDEFINED, now);
			} else if (leftResult == ContextManager.TRUE
					|| rightResult == ContextManager.TRUE) {
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
			mDeferUntil = Math.min(mLeftExpression.getDeferUntil(),
					mRightExpression.getDeferUntil());
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
			return "(" + mLeftExpression.toParseString()
					+ " " + mOperator + " "
					+ mLeftExpression.toParseString() +")";
		}
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

}
