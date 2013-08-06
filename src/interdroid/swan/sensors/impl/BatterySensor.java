package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

/**
 * A sensor for battery temperature, level and voltage.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class BatterySensor extends AbstractVdbSensor {

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.battery_preferences;
		}

	}

	/**
	 * The level field.
	 */
	public static final String LEVEL_FIELD = "level";
	/**
	 * The voltage field.
	 */
	public static final String VOLTAGE_FIELD = "voltage";
	/**
	 * The temperature field.
	 */
	public static final String TEMPERATURE_FIELD = "temperature";
	/**
	 * The plugged field
	 */
	public static final String PLUGGED_FIELD = "plugged";
	
	/**
	 * The pluggedText field
	 */
	public static final String STATUS_TEXT_FIELD = "status_text";

	/**
	 * The schema for this sensor.
	 */
	public static final String SCHEME = getSchema();

	/**
	 * The provider for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class Provider extends AvroContentProviderProxy {

		/**
		 * Construct the provider for this sensor.
		 */
		public Provider() {
			super(SCHEME);
		}

	}

	/**
	 * @return the schema for this sensor.
	 */
	private static String getSchema() {
		String scheme = "{'type': 'record', 'name': 'battery', "
				+ "'namespace': 'interdroid.context.sensor.battery',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + "\n{'name': '"
				+ LEVEL_FIELD + "', 'type': 'int'}," + "\n{'name': '"
				+ VOLTAGE_FIELD + "', 'type': 'int'}," + "\n{'name': '"
				+ PLUGGED_FIELD + "', 'type': 'int'}," + "\n{'name': '"
				+ STATUS_TEXT_FIELD + "', 'type': 'string'}," + "\n{'name': '"
				+ TEMPERATURE_FIELD + "', 'type': 'int'}" + "\n]" + "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The receiver for battery events.
	 */
	private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				long now = System.currentTimeMillis();

				ContentValues values = new ContentValues();
				values.put(LEVEL_FIELD,
						intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));
				values.put(TEMPERATURE_FIELD,
						intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0));
				values.put(VOLTAGE_FIELD,
						intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0));
				values.put(PLUGGED_FIELD,
						intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0));
				values.put(STATUS_TEXT_FIELD,
						pluggedAsText(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)));
				putValues(values, now);
			}
		}
		
		private String pluggedAsText(int i){
			switch (i) {
			case 0:
				return "Charger not plugged";
			case 1:
				return "AC Charger plugged";
			case 2:
				return "USB Charger plugged";
			default:
				return "Unknown";
			}
		}

	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { TEMPERATURE_FIELD, LEVEL_FIELD, VOLTAGE_FIELD,
				PLUGGED_FIELD, STATUS_TEXT_FIELD };
	}

	@Override
	public void initDefaultConfiguration(final Bundle defaults) {
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(batteryReceiver, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
		}
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(batteryReceiver);
		}
	}

	@Override
	public final void onDestroySensor() {
		if (registeredConfigurations.size() > 0) {
			unregisterReceiver(batteryReceiver);
		}
	}

}
