package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Abstract class that implements basic functionality for sensors. Descendants
 * only have to implement requestReading() and onEntityServiceLevelChange(). The
 * rest can be overridden optionally.
 */
public abstract class AbstractMemorySensor extends AbstractSensorBase {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(AbstractMemorySensor.class);

	/**
	 * The map of values for this sensor.
	 */
	private Map<String, List<TimestampedValue>> values =
			new HashMap<String, List<TimestampedValue>>();


	/**
	 * @return the values
	 */
	public Map<String, List<TimestampedValue>> getValues() {
		return values;
	}

	public void init() {
		for (String valuePath : VALUE_PATHS) {
			expressionIdsPerValuePath.put(valuePath, new ArrayList<String>());
			getValues().put(valuePath, Collections
					.synchronizedList(new ArrayList<TimestampedValue>()));
		}
	}

	protected void trimValues(int history) {
		for (String path : VALUE_PATHS) {
			if (getValues().get(path).size() >= history) {
				getValues().get(path).remove(0);
			}
		}
	}

	protected void putValue(String valuePath, long now, long expire,
			Object value) {
		getValues().get(valuePath).add(
				new TimestampedValue(value, now, expire));
		notifyDataChanged(valuePath);
	}

	protected void trimValueByTime(long expire) {
		for (String valuePath : VALUE_PATHS) {
			while ((getValues().get(valuePath).size() > 0
					&& getValues().get(valuePath).get(0).timestamp < expire)) {
				getValues().get(valuePath).remove(0);
			}
		}
	}

	public final List<TimestampedValue> getValues(final String id,
			final long now, final long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
