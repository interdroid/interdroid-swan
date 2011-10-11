package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorInitializationFailedException;
import interdroid.contextdroid.contextservice.SensorManager;
import android.os.Parcel;

public class ConstantTypedValue extends TypedValue {

	/**
	 * 
	 */
	private static final long serialVersionUID = 662276894518278404L;

	TimestampedValue[] values;

	public ConstantTypedValue(Object constantValue) {
		values = new TimestampedValue[] { new TimestampedValue(constantValue) };
	}

	private ConstantTypedValue() {
	}

	@Override
	public TimestampedValue[] getValues(String id, long now) {
		return values;
	}

	@Override
	public long deferUntil() {
		return Long.MAX_VALUE;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelableArray(values, 0);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		values = (TimestampedValue[]) in.readParcelableArray(this.getClass()
				.getClassLoader());
	}

	/** The CREATOR. */
	public static ConstantTypedValue.Creator<ConstantTypedValue> CREATOR = new ConstantTypedValue.Creator<ConstantTypedValue>() {

		@Override
		public ConstantTypedValue createFromParcel(Parcel source) {
			ConstantTypedValue v = new ConstantTypedValue();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public ConstantTypedValue[] newArray(int size) {
			return new ConstantTypedValue[size];
		}
	};

	@Override
	public void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorInitializationFailedException {
		// nothing to do here
	}

	@Override
	public boolean hasCurrentTime() {
		return false;
	}

	@Override
	public void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException {
		// nothing to do here
	}

	public String toString() {
		return values[0].value.toString();
	}

}
