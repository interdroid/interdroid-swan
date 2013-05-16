package interdroid.swan.sensors.impl;

import interdroid.swan.sensors.AbstractSensorBase;
import interdroid.swan.swansong.TimestampedValue;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;

public class TimeSensor extends AbstractSensorBase {

	public static final String CURRENT_MS_FIELD = "current";

	public static final String TAG = "TimeSensor";

	/**
	 * The configuration activity for this sensor.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static class ConfigurationActivity extends Activity {

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setResult(
					RESULT_OK,
					getIntent().putExtra("configuration",
							TimeSensor.CURRENT_MS_FIELD));
			finish();
		}

	}

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
		result.add(new TimestampedValue(now, now));
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
