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

public class ComparisonExpression extends Expression {

	private static final String TAG = "ComparisonExpression";

	private static final long SLEEP_THRESHOLD = 60 * 1000; //

	/**
	 *
	 */
	private static final long serialVersionUID = 4074214646065165490L;

	/**
	 * The comparator used to compare the values.
	 */
	private final Comparator mComparator;

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
	 * A reference to the SensorManager, is needed to implement
	 * sleep-and-be-ready method
	 */
	private SensorManager mSensorManager;

	/**
	 * Constructs a comparison expression.
	 * 
	 * @param left
	 *            the left expression
	 * @param comparator
	 *            the comparator
	 * @param right
	 *            the right expression
	 */
	public ComparisonExpression(final Expression left,
			final Comparator comparator, final Expression right) {
		this.mLeftValue = left;
		this.mRightValue = right;
		this.mComparator = comparator;
	}

	/**
	 * Construct from a parcel.
	 * 
	 * @param in
	 *            the parcel to read values from.
	 */
	protected ComparisonExpression(final Parcel in) {
		super(in);
		mLeftValue = in.readParcelable(getClass().getClassLoader());
		mComparator = Comparator.convert(in.readInt());
		mRightValue = in.readParcelable(getClass().getClassLoader());
	}

	/**
	 * Constructs a comparison expression. This is a convenience which wraps the
	 * TypedValues in TypedValueExpressions until we refactor TypedValue to be
	 * an expression directly.
	 * 
	 * @param left
	 *            the left expression
	 * @param comparator
	 *            the comparator
	 * @param strategy
	 *            the strategy used when comparing.
	 * @param right
	 *            the right expression
	 */
	public ComparisonExpression(TypedValue leftValue, Comparator comparator,
			TypedValue rightValue) {
		mLeftValue = new TypedValueExpression(leftValue);
		mRightValue = new TypedValueExpression(rightValue);
		mComparator = comparator;
	}

	/**
	 * The CREATOR used to construct from a parcel.
	 */
	public static final Parcelable.Creator<ComparisonExpression> CREATOR = new Parcelable.Creator<ComparisonExpression>() {
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
	 * @param left
	 *            true if the side desired is the left side
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
	public final Comparator getComparator() {
		return mComparator;
	}

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		setId(id);
		mSensorManager = sensorManager;
		mLeftValue.initialize(id + ".L", sensorManager);
		mRightValue.initialize(id + ".R", sensorManager);
	}

	@Override
	public final void destroy(final String id, final SensorManager sensorManager)
			throws ContextDroidException {
		mLeftValue.destroy(id + ".L", sensorManager);
		mRightValue.destroy(id + ".R", sensorManager);
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
		if (getHistoryReductionMode().equals(HistoryReductionMode.ALL)) {
			endResult = ContextManager.TRUE;
		} else {
			endResult = ContextManager.FALSE;
		}

		int leftIndex = 0;
		int rightIndex = 0;

		// do this with highest timestamp first, so we can maximize the
		// deferuntil
		for (int leftItem = left.length - 1; leftItem >= 0; leftItem--) {
			for (int rightItem = right.length - 1; rightItem >= 0; rightItem--) {
				int tempResult = evaluateLeafItem(left[leftItem].getValue(),
						right[rightItem].getValue());
				if (getHistoryReductionMode().equals(HistoryReductionMode.ALL)) {
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
		setResult(endResult, now);

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
			Expression contextValue;
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
							+ contextValue.getHistoryLength();
				} else if (contextValue.getHistoryReductionMode() == HistoryReductionMode.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getHistoryLength();
				} else {
					mDeferUntil = 0;
				}
				break;
			case MIN_ANY:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.MIN) {
					mDeferUntil = contextValues[0].getTimestamp()
							+ contextValue.getHistoryLength();
				} else if (contextValue.getHistoryReductionMode() == HistoryReductionMode.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getHistoryLength();
				} else {
					mDeferUntil = 0;
				}
				break;
			case ANY:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.ANY) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getHistoryLength();
				} else {
					mDeferUntil = 0;
				}
				break;
			case ALL:
				if (contextValue.getHistoryReductionMode() == HistoryReductionMode.ALL) {
					mDeferUntil = contextValues[index].getTimestamp()
							+ contextValue.getHistoryLength();
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
	}

	@Override
	protected final long getDeferUntilImpl() {
		return mDeferUntil;
	}

	@Override
	protected final int getSubtypeId() {
		return COMPARISON_EXPRESSION_TYPE;
	}

	@Override
	public void sleepAndBeReadyAt(final long readyTime) {
		final long wakeupTime;
		if (getHistoryLength() > 0) {
			wakeupTime = readyTime - getHistoryLength();
		} else {
			wakeupTime = readyTime;
		}
		if (wakeupTime - System.currentTimeMillis() > SLEEP_THRESHOLD) {
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
						sleep(wakeupTime - System.currentTimeMillis());
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					try {
						initialize(ComparisonExpression.this.getId(),
								mSensorManager);
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
	public long getHistoryLength() {
		return Math.max(mLeftValue.getHistoryLength(),
				mRightValue.getHistoryLength());
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
		return mLeftValue;
	}

	/**
	 * @return the right side of this expression.
	 */
	public Expression getRightExpression() {
		return mRightValue;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return HistoryReductionMode.DEFAULT_MODE;
	}

	@Override
	public boolean isConstant() {
		return mLeftValue.isConstant() && mRightValue.isConstant();
	}

}
