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
public class ValueExpression extends Expression {
	/**
	 *
	 */
	private static final long	serialVersionUID	= 4074214646065165490L;

	/**
	 * The comparator used to compare the values.
	 */
	private final ComparatorStrategy mComparator;

	/**
	 * The left value.
	 */
	private final TypedValue mLeftValue;

	/**
	 * The right value.
	 */
	private final TypedValue mRightValue;

	/**
	 * The time until this value expression needs to be evaluated again.
	 */
	private long mDeferUntil = -1;

	/**
	 * Constructs a value expression.
	 * @param left the left value
	 * @param comparator the comparator, possibly including strategy
	 * @param right the right value
	 */
	public ValueExpression(final TypedValue left,
			final ComparatorStrategy comparator, final TypedValue right) {
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = comparator;
	}

	/**
	 * Constructs a value expression.
	 * @param left the left value
	 * @param comparator the comparator, possibly including strategy
	 * @param right the right value
	 * @deprecated Use the ComparatorStrategy version for safety
	 */
	@Deprecated
	public ValueExpression(final TypedValue left, final String comparator,
			final TypedValue right) {
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = ComparatorStrategy.parse(comparator);
		if (this.mComparator == null) {
			throw new IllegalArgumentException("Unknown comparator strategy.");
		}

	}

	/**
	 * Construct from a parcel.
	 * @param in the parcel to read values from.
	 */
	protected ValueExpression(final Parcel in) {
		super(in);
		mLeftValue = in.readParcelable(ValueExpression.class.getClassLoader());
		mComparator = ComparatorStrategy.convert(in.readInt());
		mRightValue = in.readParcelable(ValueExpression.class.getClassLoader());
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<ValueExpression> CREATOR
	= new Parcelable.Creator<ValueExpression>() {
		@Override
		public ValueExpression createFromParcel(final Parcel in) {
			return new ValueExpression(in);
		}

		@Override
		public ValueExpression[] newArray(final int size) {
			return new ValueExpression[size];
		}
	};

	/**
	 * @param left true if the side desired is the left side
	 * @return the typedValue side requested or null
	 */
	protected final TypedValue getTypedValue(final boolean left) {
		if (left) {
			return mLeftValue;
		} else {
			return mRightValue;
		}
	}

	/**
	 * @return the comparison strategy for this expression.
	 */
	protected final ComparatorStrategy getComparator() {
		return mComparator;
	}

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
					throws SensorConfigurationException,
					SensorSetupFailedException {
		setId(id);
		mLeftValue.initialize(id + ".L", sensorManager);
		if (mRightValue != null) {
			mRightValue.initialize(id + ".R", sensorManager);
		}

	}

	@Override
	public final void destroy(final String id,
			final SensorManager sensorManager)
					throws ContextDroidException {
		mLeftValue.destroy(id + ".L", sensorManager);
		if (mRightValue != null) {
			mRightValue.destroy(id + ".R", sensorManager);
		}
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
		setResult(endResult);

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

			switch (mComparator.getComparator()) {
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
			case EQUALS_NOT:
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
						+ mComparator.getComparator());
			}
		}
	}

	@Override
	protected final String toStringImpl() {
		return mLeftValue + " " + mComparator + " " + mRightValue;
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
}
