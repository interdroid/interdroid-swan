package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;
import interdroid.contextdroid.contextservice.SensorManager;
import android.location.Location;
import android.os.Parcel;

/**
 * Represents a typed value which is combined with another via a
 * mathematical operation represented by an Operation using a
 * particular Strategy.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class CombinedTypedValue extends TypedValue {
	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 7886843131653803215L;

	/** Number of axes in a location. */
	private static final int NUM_AXES = 3;

	/** The left side of the combined value. */
	private TypedValue mLeft;
	/** The right side of the combined value. */
	private TypedValue mRight;

	/** The combination operator for this expression. */
	private MathOperator mOperator;

	/**
	 * Constructs a combined typed value with history reduction.
	 * @param left the left side
	 * @param operator the operator
	 * @param right the right side.
	 */
	public CombinedTypedValue(final TypedValue left,
			final MathOperator operator, final TypedValue right) {
		super(HistoryReductionMode.NONE);
		this.mLeft = left;
		this.mRight = right;
		this.mOperator = operator;
	}

	/**
	 * Constructs from a parcel.
	 * @param source the parcel to read from.
	 */
	public CombinedTypedValue(final Parcel source) {
		super(source);
		readFromParcel(source);
	}

	@Override
	public final TimestampedValue[] getValues(final String id, final long now)
			throws ContextDroidException {

		TimestampedValue[] leftValues = mLeft.getValues(id + ".L", now);
		TimestampedValue[] rightValues = mRight.getValues(id + ".R", now);
		if (leftValues.length == 1 || rightValues.length == 1) {
			TimestampedValue[] result =
					new TimestampedValue[leftValues.length
					                     * rightValues.length];
			int index = 0;
			for (int i = 0; i < leftValues.length; i++) {
				for (int j = 0; j < rightValues.length; j++) {
					result[index++] = operate(leftValues[i], rightValues[j]);
				}
			}
			return applyMode(result);
		} else {
			throw new ContextDroidException(
					"Unable to combine two arrays, "
							+ "only one of the operands can be an array: "
							+ mOperator);
		}
	}

	/**
	 * Performs the operation on the requested values.
	 * @param left the left side
	 * @param right the right side
	 * @return the timestamped values
	 * @throws ContextDroidException if someting goes wrong
	 */
	private TimestampedValue operate(final TimestampedValue left,
			final TimestampedValue right) throws ContextDroidException {
		if (left.getValue() instanceof Double
				&& right.getValue() instanceof Double) {
			return new TimestampedValue(operateDouble(
					(Double) left.getValue(),
					(Double) right.getValue()),
					left.getTimestamp(), left.getExpireTime());
		} else if (left.getValue() instanceof Long
				&& right.getValue() instanceof Long) {
			return new TimestampedValue(operateLong(
					(Long) left.getValue(),
					(Long) right.getValue()),
					left.getTimestamp(), left.getExpireTime());
		} else if (left.getValue() instanceof String
				&& right.getValue() instanceof String) {
			return new TimestampedValue(operateString(
					(String) left.getValue(),
					(String) right.getValue()),
					left.getTimestamp(), left.getExpireTime());
		} else if (left.getValue() instanceof Location
				&& right.getValue() instanceof Location) {
			return new TimestampedValue(operateLocation(
					(Location) left.getValue(),
					(Location) right.getValue()),
					left.getTimestamp(), left.getExpireTime());
		}

		throw new ContextDroidException(
				"Trying to operate on incompatible types: "
						+ left.getValue().getClass() + " and "
						+ right.getValue().getClass());
	}

	/**
	 * Operates on doubles.
	 * @param left the left side value
	 * @param right the right side value
	 * @return the combined value
	 * @throws ContextDroidException if something goes wrong.
	 */
	private Double operateDouble(final double left, final double right)
			throws ContextDroidException {
		Double ret;
		switch (mOperator) {
		case MINUS:
			ret = left - right;
			break;
		case PLUS:
			ret = left + right;
			break;
		case TIMES:
			ret = left * right;
			break;
		case DIVIDE:
			ret = left / right;
			break;
		default:
			throw new ContextDroidException("Unknown operator: '" + mOperator
					+ "' for type Double");
		}
		return ret;
	}

	/**
	 * Operates on longs.
	 * @param left the left side value
	 * @param right the right side value
	 * @return the combined value
	 * @throws ContextDroidException if something goes wrong.
	 */
	private Long operateLong(final long left, final long right)
			throws ContextDroidException {
		Long ret;
		switch (mOperator) {
		case MINUS:
			ret = left - right;
			break;
		case PLUS:
			ret = left + right;
			break;
		case TIMES:
			ret = left * right;
			break;
		case DIVIDE:
			ret = left / right;
			break;
		default:
			throw new ContextDroidException("Unknown operator: '" + mOperator
					+ "' for type Long");
		}
		return ret;
	}

	/**
	 * Operates on string.
	 * @param left the left side value
	 * @param right the right side value
	 * @return the combined value
	 * @throws ContextDroidException if something goes wrong.
	 */
	private String operateString(final String left, final String right)
			throws ContextDroidException {
		String ret;
		switch (mOperator) {
		case PLUS:
			ret = left + right;
			break;
		default:
			throw new ContextDroidException("Unknown operator: '" + mOperator
					+ "' for type String");
		}
		return ret;
	}

	/**
	 * Operates on locations.
	 * @param left the left side value
	 * @param right the right side value
	 * @return the combined value
	 * @throws ContextDroidException if something goes wrong.
	 */
	private Float operateLocation(final Location left, final Location right)
			throws ContextDroidException {
		Float ret;
		switch (mOperator) {
		case MINUS:
			float[] results = new float[NUM_AXES];
			Location.distanceBetween(left.getLatitude(), left.getLongitude(),
					right.getLatitude(), right.getLongitude(), results);
			ret = results[0];
			break;
		default:
			throw new ContextDroidException("Unknown operator: '" + mOperator
					+ "' for type Location");
		}
		return ret;
	}

	@Override
	public final long getDeferUntil() {
		return Math.min(mLeft.getDeferUntil(), mRight.getDeferUntil());
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	protected final void writeSubclassToParcel(final Parcel dest,
			final int flags) {
		dest.writeParcelable(mLeft, 0);
		dest.writeParcelable(mRight, 0);
		dest.writeInt(mOperator.convert());
	}

	/**
	 * Read from parcel.
	 *
	 * @param in
	 *            the Parcel to read from.
	 */
	private void readFromParcel(final Parcel in) {
		mLeft = in.readParcelable(this.getClass().getClassLoader());
		mRight = in.readParcelable(this.getClass().getClassLoader());
		mOperator = MathOperator.convert(in.readInt());
	}

	/** The CREATOR. */
	public static final CombinedTypedValue.Creator<CombinedTypedValue> CREATOR =
			new CombinedTypedValue.Creator<CombinedTypedValue>() {

		@Override
		public CombinedTypedValue createFromParcel(final Parcel source) {
			CombinedTypedValue v = new CombinedTypedValue(source);
			return v;
		}

		@Override
		public CombinedTypedValue[] newArray(final int size) {
			return new CombinedTypedValue[size];
		}
	};

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
					throws SensorConfigurationException,
					SensorSetupFailedException {
		mLeft.initialize(id + ".L", sensorManager);
		mRight.initialize(id + ".R", sensorManager);
	}

	@Override
	public final void destroy(final String id,
			final SensorManager sensorManager)
					throws ContextDroidException {
		mLeft.destroy(id + ".L", sensorManager);
		mRight.destroy(id + ".R", sensorManager);
	}

	@Override
	protected final boolean hasCurrentTime() {
		return mLeft.hasCurrentTime() || mRight.hasCurrentTime();
	}

	@Override
	public final String toParseString() {
		return mLeft.toParseString() + " " + mOperator.toParseString()
				+ " " + mRight.toParseString();
	}

}
