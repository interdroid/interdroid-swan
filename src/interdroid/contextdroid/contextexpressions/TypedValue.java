package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorInitializationFailedException;
import interdroid.contextdroid.contextservice.SensorManager;

import java.io.Serializable;

import android.os.Parcelable;

public abstract class TypedValue implements Serializable, Parcelable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2920550668593484018L;

	public enum HistoryReductionMode {
		NONE, MAX, MIN, MEAN, MEDIAN
	};

	HistoryReductionMode mode;

	public abstract TimestampedValue[] getValues(String id, long now)
			throws ContextDroidException, NoValuesInIntervalException;

	public abstract long deferUntil();

	public TimestampedValue[] applyMode(TimestampedValue[] values) {
		switch (mode) {
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

	public abstract void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorInitializationFailedException;

	public abstract void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException;

	public abstract boolean hasCurrentTime();
}
