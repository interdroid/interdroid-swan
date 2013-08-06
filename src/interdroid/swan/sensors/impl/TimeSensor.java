package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractSensorBase;
import interdroid.swan.swansong.TimestampedValue;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;

public class TimeSensor extends AbstractSensorBase {

	public static final String CURRENT_MS_FIELD = "current";
	public static final String DAY_OF_WEEK_FIELD = "day_of_week";
	public static final String HOUR_OF_DAY_FIELD = "hour_of_day";
	public static final String CUSTOM_FIELD = "custom";

	public static final String TAG = "TimeSensor";

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * @author roelof &lt;rkemp@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public int getPreferencesXML() {
			return R.xml.time_preferences;
		}

	}

	@Override
	public String[] getValuePaths() {
		return new String[] { CURRENT_MS_FIELD, DAY_OF_WEEK_FIELD,
				HOUR_OF_DAY_FIELD, CUSTOM_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle defaultConfig) {
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
	public List<TimestampedValue> getValues(String id, long now, long timespan) {
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
