package interdroid.swan.contextexpressions;

import interdroid.swan.SwanException;
import interdroid.swan.contextservice.SensorConfigurationException;
import interdroid.swan.contextservice.SensorSetupFailedException;
import interdroid.swan.contextservice.SensorManager;
import android.os.Parcel;

/**
 * Represents a typed expression which is a constant.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class ConstantTypedValue extends TypedValue {

	/**
	 * Serial version uid
	 */
	private static final long serialVersionUID = 662276894518278404L;

	/**
	 * The values of this constant.
	 */
	private TimestampedValue[] values;

	/**
	 * Constructs a constant with the given value.
	 * 
	 * @param constantValue
	 *            the value of the constant.
	 */
	public ConstantTypedValue(final Object constantValue) {
		super(HistoryReductionMode.DEFAULT_MODE);
		values = new TimestampedValue[] { new TimestampedValue(constantValue) };
	}

	/**
	 * Construct via Parcelable interface.
	 * 
	 * @param saved
	 *            the saved values to read from.
	 */
	private ConstantTypedValue(final Parcel saved) {
		super(saved);
		readFromParcel(saved);
	}

	@Override
	public final TimestampedValue[] getValues(final String id, final long now) {
		return values;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	protected final void writeSubclassToParcel(final Parcel dest,
			final int flags) {
		dest.writeParcelable(values[0], flags);

		// dest.writeParcelableArray(values, flags);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public final void readFromParcel(final Parcel in) {
		values = new TimestampedValue[] { in.readParcelable(getClass()
				.getClassLoader()) };
		// values = (TimestampedValue[]) in.readParcelableArray(this.getClass()
		// .getClassLoader());
	}

	/** The CREATOR. */
	public static final ConstantTypedValue.Creator<ConstantTypedValue> CREATOR = new ConstantTypedValue.Creator<ConstantTypedValue>() {

		@Override
		public ConstantTypedValue createFromParcel(final Parcel source) {
			ConstantTypedValue v = new ConstantTypedValue(source);
			return v;
		}

		@Override
		public ConstantTypedValue[] newArray(final int size) {
			return new ConstantTypedValue[size];
		}
	};

	@Override
	public void initialize(final String id, final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		// nothing to do here
	}

	@Override
	protected final boolean hasCurrentTime() {
		return false;
	}

	@Override
	public void destroy(final String id, final SensorManager sensorManager)
			throws SwanException {
		// nothing to do here
	}

	@Override
	public final String toString() {
		return values[0].getValue().toString();
	}

	@Override
	public final String toParseString() {
		if (values[0].getValue() instanceof String) {
			return "'" + values[0].getValue().toString() + "'";
		} else {
			return values[0].getValue().toString();
		}
	}

	@Override
	public boolean isConstant() {
		return true;
	}

	@Override
	public long getHistoryLength() {
		return 0;
	}

}
