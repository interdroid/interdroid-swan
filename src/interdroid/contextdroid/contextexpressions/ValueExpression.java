package interdroid.contextdroid.contextexpressions;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
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

	private static final String TAG = "ValueExpression";

	private static final long SLEEP_THRESHOLD = 10 * 1000; //

	/**
	 *
	 */
	private static final long serialVersionUID = 4074214646065165490L;

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
	 * A reference to the SensorManager, is needed to implement
	 * sleep-and-be-ready method
	 */
	private SensorManager mSensorManager;

	/**
	 * Constructs a value expression.
	 * 
	 * @param left
	 *            the left value
	 * @param comparator
	 *            the comparator
	 * @param strategy
	 *            the strategy used when comparing.
	 * @param right
	 *            the right value
	 */
	public ValueExpression(final TypedValue left, final Comparator comparator,
			final Strategy strategy, final TypedValue right) {
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = comparator;
		this.mStrategy = strategy;
	}

	/**
	 * Construct from a parcel.
	 * 
	 * @param in
	 *            the parcel to read values from.
	 */
	protected ValueExpression(final Parcel in) {
		super(in);
		mLeftValue = in.readParcelable(ValueExpression.class.getClassLoader());
		mComparator = Comparator.convert(in.readInt());
		mStrategy = Strategy.convert(in.readInt());
		mRightValue = in.readParcelable(ValueExpression.class.getClassLoader());
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<ValueExpression> CREATOR = new Parcelable.Creator<ValueExpression>() {
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
	 * @param left
	 *            true if the side desired is the left side
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
			throws SensorConfigurationException, SensorSetupFailedException {
		setId(id);
		mSensorManager = sensorManager;
		mLeftValue.initialize(id + ".L", sensorManager);
		if (mRightValue != null) {
			mRightValue.initialize(id + ".R", sensorManager);
		}

	}

	@Override
	public final void destroy(final String id, final SensorManager sensorManager)
			throws ContextDroidException {
		mLeftValue.destroy(id + ".L", sensorManager);
		if (mRightValue != null) {
			mRightValue.destroy(id + ".R", sensorManager);
		}
	}

	/**
	 * Evaluates this value expression.
	 * 
	 * @param now
	 *            the time to evaluate at
	 * @throws ContextDroidException
	 *             if something goes wrong.
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

		int leftIndex = 0;
		int rightIndex = 0;

		// do this with highest timestamp first, so we can maximize the
		// deferuntil
		for (int leftItem = left.length - 1; leftItem >= 0; leftItem--) {
			for (int rightItem = left.length - 1; rightItem >= 0; rightItem--) {
				int tempResult = evaluateLeafItem(left[leftItem].getValue(),
						right[rightItem].getValue());
				if (mStrategy.equals(Strategy.ALL)) {
					if (tempResult == ContextManager.FALSE) {
						endResult = ContextManager.FALSE;
						leftIndex = leftItem;
						rightIndex = rightItem;
						break;
					} else if (tempResult == ContextManager.UNDEFINED) {
						endResult = ContextManager.UNDEFINED;
					}
				} else {
					if (tempResult == ContextManager.TRUE) {
						endResult = ContextManager.TRUE;
						leftIndex = leftItem;
						rightIndex = rightItem;
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
		calculateDeferUntil(now, left, right, leftIndex, rightIndex);
	}

	/**
	 * Evaluates a leaf item performing the comparison.
	 * 
	 * @param left
	 *            the left side values
	 * @param right
	 *            the right side values
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

	private static Mode[][][] mDeferStrategy = new Mode[2][2][Comparator
			.values().length];
	private static int LEFT_CONST = 0;
	private static int RIGHT_CONST = 1;

	private static enum Mode {
		MAX_ANY, MIN_ANY, ANY, ALL
	}

	static {
		for (Comparator comparator : Comparator.values()) {
			switch (comparator) {
			case GREATER_THAN:
			case GREATER_THAN_OR_EQUALS:
				mDeferStrategy[LEFT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.MAX_ANY;
				mDeferStrategy[LEFT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.MIN_ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.MIN_ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.MAX_ANY;
				break;
			case LESS_THAN:
			case LESS_THAN_OR_EQUALS:
				mDeferStrategy[LEFT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.MIN_ANY;
				mDeferStrategy[LEFT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.MAX_ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.MAX_ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.MIN_ANY;
				break;
			case EQUALS:
			case REGEX_MATCH:
			case STRING_CONTAINS:
				mDeferStrategy[LEFT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.ANY;
				mDeferStrategy[LEFT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.ALL;
				mDeferStrategy[RIGHT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.ALL;
				break;
			case NOT_EQUALS:
				mDeferStrategy[LEFT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.ALL;
				mDeferStrategy[LEFT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.ANY;
				mDeferStrategy[RIGHT_CONST][ContextManager.TRUE][comparator
						.convert()] = Mode.ALL;
				mDeferStrategy[RIGHT_CONST][ContextManager.FALSE][comparator
						.convert()] = Mode.ANY;
				break;
			default:
				throw new RuntimeException("Unknown Comparator.");
			}
		}
	}

	/**
	 * Sets the result for the given expression.
	 * 
	 * @param now
	 *            the time that was evaluated
	 * @param left
	 *            the left values
	 * @param right
	 *            the right values
	 */
	private void calculateDeferUntil(final long now,
			final TimestampedValue[] left, final TimestampedValue[] right,
			final int leftIndex, final int rightIndex) {

		// If undefined then zero;
		if (getResult() == ContextManager.UNDEFINED) {
			mDeferUntil = 0;
			// If both constant then infinite;
		} else if (mLeftValue.isConstant() && mRightValue.isConstant()) {
			mDeferUntil = Long.MAX_VALUE;
		} else if (!mLeftValue.isConstant() && !mRightValue.isConstant()) {
			mDeferUntil = 0;
		} else {
			Mode mode;
			TypedValue contextValue;
			TimestampedValue[] contextValues;
			int index;
			if (mLeftValue.isConstant()) {
				mode = mDeferStrategy[LEFT_CONST][getResult()][mComparator
						.convert()];
				contextValue = mRightValue;
				contextValues = right;
				index = rightIndex;
			} else {
				mode = mDeferStrategy[RIGHT_CONST][getResult()][mComparator
						.convert()];
				contextValue = mLeftValue;
				contextValues = left;
				index = leftIndex;
			}

			switch (mode) {
			case MAX_ANY:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.MAX) {
					mDeferUntil = contextValues[0].getTimestamp()
							+ contextValue.getTimespan();
				} else if (contextValue.getHistoryReductionMode() == HistoryReductionMode.NONE
						&& mStrategy == Strategy.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getTimespan();
				} else {
					mDeferUntil = 0;
				}
				break;
			case MIN_ANY:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.MIN) {
					mDeferUntil = contextValues[0].getTimestamp()
							+ contextValue.getTimespan();
				} else if (contextValue.getHistoryReductionMode() == HistoryReductionMode.NONE
						&& mStrategy == Strategy.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getTimespan();
				} else {
					mDeferUntil = 0;
				}
				break;
			case ANY:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.NONE
						&& mStrategy == Strategy.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getTimespan();
				} else {
					mDeferUntil = 0;
				}
				break;
			case ALL:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.NONE
						&& mStrategy == Strategy.ALL) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getTimespan();
				} else {
					mDeferUntil = 0;
				}
				break;
			default:
				throw new RuntimeException("Unknown mode.");
			}
		}

		if (!(!mLeftValue.hasCurrentTime() && !mRightValue.hasCurrentTime())) {
			// TODO: think about when left or right is an array
			long leftTime = (Long) left[0].getValue();
			long rightTime = (Long) right[0].getValue();

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
		return mLeftValue.toParseString() + " " + mComparator.toParseString()
				+ " " + mRightValue.toParseString();
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
	public void sleepAndBeReadyAt(long readyTime) {
		if (readyTime - System.currentTimeMillis() > SLEEP_THRESHOLD) {
			try {
				mLeftValue.destroy(getId() + ".L", mSensorManager);
				mRightValue.destroy(getId() + ".R", mSensorManager);
			} catch (ContextDroidException e1) {
				Log.e(TAG, "Failed to stop sensor", e1);
				return;
			}

			// TODO: look at this, should this be implemented with the Android
			// Alarm system?
			// restart Thread
			new Thread() {
				public void run() {
					try {
						initialize(ValueExpression.this.getId(), mSensorManager);
					} catch (SensorConfigurationException e) {
						Log.e(TAG, "Failed to re-initialize sensor", e);
					} catch (SensorSetupFailedException e) {
						Log.e(TAG,
								"Failed to setup for re-initialization sensor",
								e);
					}
				}
			}.start();
		}
	}

	@Override
	public long getTimespan() {
		return Math.max(mLeftValue.getTimespan(), mRightValue.getTimespan());
	}

}
