package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractAsynchronousSensor;
import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.contextdroid.sensors.AbstractAsynchronousSensor;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

public class TimeSensor extends AbstractAsynchronousSensor {

	public static final String CURRENT_MS_FIELD = "current";

	public static final String TAG = "TimeSensor";

	@Override
	public String[] getValuePaths() {
		return new String[] { CURRENT_MS_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'time', 'namespace': 'context.sensor',"
				+ " 'fields': [{'name': '"
				+ CURRENT_MS_FIELD
				+ "', 'type': 'long'}]" + "}".replace('\'', '"');
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
	}

	@Override
	protected void unregister(String id) {
	}

	@Override
	protected List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();
		result.add(new TimestampedValue(now, now, 0));
		return result;
	}

}
