package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractSensorBase;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

public class TimeSensor extends AbstractSensorBase {

	public static final String CURRENT_MS_FIELD = "current";

	public static final String TAG = "TimeSensor";

	@Override
	public String[] getValuePaths() {
		return new String[] { CURRENT_MS_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle defaultConfig) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'time', 'namespace': 'context.sensor',"
				+ " 'fields': [{'name': '"
				+ CURRENT_MS_FIELD
				+ "', 'type': 'long'}]" + "}".replace('\'', '"');
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		// Nothing to do
	}

	@Override
	public final void unregister(String id) {
		// Nothing to do
	}

	@Override
	public List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();
		result.add(new TimestampedValue(now, now, 0));
		return result;
	}

	@Override
	public final void onDestroySensor() {
		// Nothing to do
	}

	@Override
	public final void onConnected() {
		// Nothing to do
	}

	@Override
	protected void init() {
		// Nothing to do
	}

}
