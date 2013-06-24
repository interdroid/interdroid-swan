package interdroid.swan.sensors;

import java.util.Map;

import android.content.ContentValues;

public class MonitorThread extends Thread {

	private AbstractCuckooSensor sensor;
	private String valuePath;
	private Map<String, Object> configuration;

	public MonitorThread(AbstractCuckooSensor sensor, final String valuePath,
			final Map<String, Object> configuration) {
		this.sensor = sensor;
		this.valuePath = valuePath;
		this.configuration = configuration;
	}

	public void run() {
		Map<String, Object> previous = null;
		while (!interrupted()) {
			Map<String, Object> values = sensor.getPoller().poll(valuePath,
					configuration);
			if (changed(previous, values)) {
				System.out.println("change! " + previous + " -> " + values);
				previous = values;
				sensor.putValues(toContentValues(values),
						System.currentTimeMillis());
			}
			try {
				sleep(sensor.getPoller().getInterval(configuration, false));
			} catch (InterruptedException e) {
				// ignore, we will exit the loop anyways
			}
		}
	}

	private boolean changed(Map<String, Object> old, Map<String, Object> current) {
		if (current == null) {
			// new values are not valid
			return false;
		} else if (old == null) {
			// old values were invalid
			return true;
		} else {
			for (String key : old.keySet()) {
				if (!old.get(key).equals(current.get(key))) {
					// yes, we found a change
					return true;
				}
			}
		}
		return false;
	}

	private ContentValues toContentValues(Map<String, Object> map) {
		ContentValues values = new ContentValues();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof Boolean) {
				values.put(key, (Boolean) value);
			} else if (value instanceof Byte) {
				values.put(key, (Byte) value);
			} else if (value instanceof byte[]) {
				values.put(key, (byte[]) value);
			} else if (value instanceof Double) {
				values.put(key, (Double) value);
			} else if (value instanceof Float) {
				values.put(key, (Float) value);
			} else if (value instanceof Integer) {
				values.put(key, (Integer) value);
			} else if (value instanceof Long) {
				values.put(key, (Long) value);
			} else if (value instanceof Short) {
				values.put(key, (Short) value);
			} else if (value instanceof String) {
				values.put(key, (String) value);
			} else {
				throw new RuntimeException(
						"Impossible to convert type in map to contentvalues: "
								+ value);
			}
		}
		return values;
	}

}
