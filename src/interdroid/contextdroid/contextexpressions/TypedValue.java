package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;
import interdroid.contextdroid.contextservice.SensorManager;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a value of a particular type within an expression.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public abstract class TypedValue implements Serializable, Parcelable {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = -2920550668593484018L;

	/**
	 * The reduction mode this typed value runs with.
	 */
	private HistoryReductionMode mMode;

	/**
	 * Constructs a typed value with the given reduction mode.
	 * @param mode the reduction mode.
	 */
	public TypedValue(final HistoryReductionMode mode) {
		mMode = mode;
	}

	/**
	 * Construct from a parcel.
	 * @param source the parcel to get data from.
	 */
	public TypedValue(final Parcel source) {
		readFromParcel(source);
	}

	/**
	 * @param id the id of the expression.
	 * @param now the time to evaluate at
	 * @return the values for this typed value
	 * @throws ContextDroidException if something goes wrong.
	 */
	public abstract TimestampedValue[] getValues(final String id,
			final long now)
			throws ContextDroidException;

	/**
	 * @return the time until this should be evaluated again.
	 */
	public abstract long getDeferUntil();

	/**
	 * Applies the reduction mode to the values.
	 * @param values the value to reduce
	 * @return the reduced values.
	 */
	public final TimestampedValue[] applyMode(final TimestampedValue[] values) {
		switch (mMode) {
		case MAX:
			return new TimestampedValue[] { TimestampedValue
					.findMaxValue(values) };
		case MIN:
			return new TimestampedValue[] { TimestampedValue
					.findMinValue(values) };
		case MEAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMean(values) };
		case MEDIAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMedian(values) };
		case NONE:
		default:
			return values;
		}
	}

	/**
	 * Initializes this value with the sensor manager.
	 * @param id the id of the expression
	 * @param sensorManager the sensor manager to init with
	 * @throws SensorConfigurationException if the config is problematic
	 * @throws SensorSetupFailedException if there is a problem.
	 */
	public abstract void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorSetupFailedException;

	/**
	 * Destroys these values.
	 * @param id the id of the expression
	 * @param sensorManager the sensor manager to work with
	 * @throws ContextDroidException if something goes wrong.
	 */
	public abstract void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException;

	/**
	 * @return true if this value has the current time.
	 */
	protected abstract boolean hasCurrentTime();

	/**
	 * Sets values from a parcel.
	 * @param source parcel to read from
	 */
	private void readFromParcel(final Parcel source) {
		mMode = HistoryReductionMode.convert(source.readInt());
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(mMode.convert());
		writeSubclassToParcel(dest, flags);
	}

	/**
	 * Interface for subclasses to get involved in parceling an instance.
	 * @param dest the parcel to write to
	 * @param flags the flags for writing
	 */
	protected abstract void writeSubclassToParcel(Parcel dest, int flags);
}
