package interdroid.swan.sensors;

import interdroid.swan.swansong.TimestampedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that implements basic functionality for sensors. Descendants
 * only have to implement requestReading() and onEntityServiceLevelChange(). The
 * rest can be overridden optionally.
 */
public abstract class AbstractMemorySensor extends AbstractSensorBase {

	/**
	 * The map of values for this sensor.
	 */
	private final Map<String, List<TimestampedValue>> values = new HashMap<String, List<TimestampedValue>>();

	/**
	 * @return the values
	 */
	public final Map<String, List<TimestampedValue>> getValues() {
		return values;
	}

	@Override
	public final void init() {
		for (String valuePath : VALUE_PATHS) {
			expressionIdsPerValuePath.put(valuePath, new ArrayList<String>());
			getValues()
					.put(valuePath,
							Collections
									.synchronizedList(new ArrayList<TimestampedValue>()));
		}
	}

	/**
	 * Trims the values to the given length.
	 * 
	 * @param history
	 *            the number of items to keep
	 */
	private final void trimValues(final int history) {
		for (String path : VALUE_PATHS) {
			if (getValues().get(path).size() >= history) {
				getValues().get(path).remove(getValues().get(path).size() - 1);
			}
		}
	}

	/**
	 * Adds a value for the given value path to the history.
	 * 
	 * @param valuePath
	 *            the value path
	 * @param now
	 *            the current time
	 * @param value
	 *            the value
	 * @param historySize
	 *            the history size
	 */
	protected final void putValueTrimSize(final String valuePath,
			final String id, final long now, final Object value,
			final int historySize) {
		getValues().get(valuePath).add(0, new TimestampedValue(value, now));
		trimValues(historySize);
		if (id != null) {
			notifyDataChangedForId(id);
		} else {
			notifyDataChanged(valuePath);
		}
	}

	/**
	 * Adds a value for the given value path to the history.
	 * 
	 * @param valuePath
	 *            the value path
	 * @param now
	 *            the current time
	 * @param value
	 *            the value
	 * @param historyLength
	 *            the history length
	 */
	protected final void putValueTrimTime(final String valuePath,
			final String id, final long now, final Object value,
			final long historyLength) {
		getValues().get(valuePath).add(0, new TimestampedValue(value, now));
		trimValueByTime(now - historyLength);
		if (id != null) {
			notifyDataChangedForId(id);
		} else {
			notifyDataChanged(valuePath);
		}
	}

	/**
	 * Trims values past the given expire time.
	 * 
	 * @param expire
	 *            the time to trim after
	 */
	private final void trimValueByTime(final long expire) {
		for (String valuePath : VALUE_PATHS) {
			List<TimestampedValue> values = getValues().get(valuePath);
			while ((values.size() > 0 && values.get(values.size() - 1)
					.getTimestamp() < expire)) {
				values.remove(values.size() - 1);
			}
		}
	}

	@Override
	public final List<TimestampedValue> getValues(final String id,
			final long now, final long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
