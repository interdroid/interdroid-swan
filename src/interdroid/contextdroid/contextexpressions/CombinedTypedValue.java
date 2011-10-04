package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorInitializationFailedException;
import interdroid.contextdroid.contextservice.SensorManager;
import android.location.Location;
import android.os.Parcel;

public class CombinedTypedValue extends TypedValue {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7886843131653803215L;

	TypedValue left;
	TypedValue right;

	String operator;

	public CombinedTypedValue(TypedValue left, String operator, TypedValue right) {
		this(left, operator, right, HistoryReductionMode.NONE);
	}

	public CombinedTypedValue(TypedValue left, String operator,
			TypedValue right, HistoryReductionMode mode) {
		this.left = left;
		this.right = right;
		this.operator = operator;
		this.mode = mode;
	}

	private CombinedTypedValue() {
	}

	@Override
	public TimestampedValue[] getValues(String id, long now)
			throws ContextDroidException, NoValuesInIntervalException {

		TimestampedValue[] leftValues = left.getValues(id + ".L", now);
		TimestampedValue[] rightValues = right.getValues(id + ".R", now);
		if (leftValues.length == 1 || rightValues.length == 1) {
			TimestampedValue[] result = new TimestampedValue[leftValues.length
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
					"Unable to combine two arrays, only one of the operands can be an array: "
							+ operator);
		}
	}

	public TimestampedValue operate(TimestampedValue left,
			TimestampedValue right) throws ContextDroidException {
		if (left.value instanceof Double && right.value instanceof Double) {
			return new TimestampedValue(operateDouble((Double) left.value,
					(Double) right.value), left.timestamp, left.expireTime);
		} else if (left.value instanceof Long && right.value instanceof Long) {
			return new TimestampedValue(operateLong((Long) left.value,
					(Long) right.value), left.timestamp, left.expireTime);
		} else if (left.value instanceof String
				&& right.value instanceof String) {
			return new TimestampedValue(operateString((String) left.value,
					(String) right.value), left.timestamp, left.expireTime);
		} else if (left.value instanceof Location
				&& right.value instanceof Location) {
			return new TimestampedValue(operateLocation((Location) left.value,
					(Location) right.value), left.timestamp, left.expireTime);
		}

		throw new ContextDroidException(
				"Trying to operate on incompatible types: "
						+ left.value.getClass() + " and "
						+ right.value.getClass());
	}

	private Double operateDouble(double left, double right)
			throws ContextDroidException {
		if ("-".equals(operator)) {
			return left - right;
		} else if ("+".equals(operator)) {
			return left + right;
		} else if ("*".equals(operator)) {
			return left * right;
		} else if ("/".equals(operator)) {
			return left / right;
		}
		throw new ContextDroidException("Unknown operator: '" + operator
				+ "' for type Double");
	}

	private Long operateLong(long left, long right)
			throws ContextDroidException {
		if ("-".equals(operator)) {
			return left - right;
		} else if ("+".equals(operator)) {
			return left + right;
		} else if ("*".equals(operator)) {
			return left * right;
		} else if ("/".equals(operator)) {
			return left / right;
		}
		throw new ContextDroidException("Unknown operator: '" + operator
				+ "' for type Long");
	}

	private String operateString(String left, String right)
			throws ContextDroidException {
		if ("+".equals(operator)) {
			return left + right;
		}
		throw new ContextDroidException("Unknown operator: '" + operator
				+ "' for type String");
	}

	private Float operateLocation(Location left, Location right)
			throws ContextDroidException {
		if ("-".equals(operator)) {
			float[] results = new float[3];
			Location.distanceBetween(left.getLatitude(), left.getLongitude(),
					right.getLatitude(), right.getLongitude(), results);
			return results[0];
		}
		throw new ContextDroidException("Unknown operator: '" + operator
				+ "' for type Location");
	}

	@Override
	public long deferUntil() {
		return Math.min(left.deferUntil(), right.deferUntil());
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(left, 0);
		dest.writeParcelable(right, 0);
		dest.writeString(operator);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		left = in.readParcelable(this.getClass().getClassLoader());
		right = in.readParcelable(this.getClass().getClassLoader());
		operator = in.readString();
	}

	/** The CREATOR. */
	public static CombinedTypedValue.Creator<CombinedTypedValue> CREATOR = new CombinedTypedValue.Creator<CombinedTypedValue>() {

		@Override
		public CombinedTypedValue createFromParcel(Parcel source) {
			CombinedTypedValue v = new CombinedTypedValue();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public CombinedTypedValue[] newArray(int size) {
			return new CombinedTypedValue[size];
		}
	};

	@Override
	public void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorInitializationFailedException {
		left.initialize(id + ".L", sensorManager);
		right.initialize(id + ".R", sensorManager);
	}

	@Override
	public void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException {
		left.destroy(id + ".L", sensorManager);
		right.destroy(id + ".R", sensorManager);
	}

	@Override
	public boolean hasCurrentTime() {
		return left.hasCurrentTime() || right.hasCurrentTime();
	}

}
