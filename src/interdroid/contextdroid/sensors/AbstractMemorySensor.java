package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

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
	private final Map<String, List<TimestampedValue>> values =
			new HashMap<String, List<TimestampedValue>>();


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
			getValues().put(valuePath, Collections
					.synchronizedList(new ArrayList<TimestampedValue>()));
		}
	}

	/**
	 * Trims the values to the given length.
	 * @param history the number of items to keep
	 */
	protected final void trimValues(final int history) {
		for (String path : VALUE_PATHS) {
			if (getValues().get(path).size() >= history) {
				getValues().get(path).remove(0);
			}
		}
	}

	/**
	 * Adds a value for the given value path to the history.
	 * @param valuePath the value path
	 * @param now the current time
	 * @param expire the expire time for the value
	 * @param value the value
	 */
	protected final void putValue(final String valuePath, final long now,
			final long expire, final Object value) {
		getValues().get(valuePath).add(
				new TimestampedValue(value, now, expire));
		notifyDataChanged(valuePath);
	}

	/**
	 * Trims values past the given expire time.
	 * @param expire the time to trim after
	 */
	protected final void trimValueByTime(final long expire) {
		for (String valuePath : VALUE_PATHS) {
			while ((getValues().get(valuePath).size() > 0
					&& getValues().get(
							valuePath).get(0).getTimestamp() < expire)) {
				getValues().get(valuePath).remove(0);
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
