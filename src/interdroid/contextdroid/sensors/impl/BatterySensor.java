package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
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
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static class ConfigurationActivity
	extends AbstractConfigurationActivity {

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
	 * The default expiration time for readings.
	 */
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 30 minutes?

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
		String scheme =
				"{'type': 'record', 'name': 'battery', "
						+ "'namespace': 'interdroid.context.sensor.battery',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ LEVEL_FIELD
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ VOLTAGE_FIELD
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ TEMPERATURE_FIELD
						+ "', 'type': 'int'}"
						+ "\n]"
						+ "}";
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
				long expire = now + EXPIRE_TIME;

				ContentValues values = new ContentValues();
				values.put(LEVEL_FIELD, intent.getIntExtra(
						BatteryManager.EXTRA_LEVEL, 0));
				values.put(TEMPERATURE_FIELD, intent.getIntExtra(
						BatteryManager.EXTRA_TEMPERATURE, 0));
				values.put(VOLTAGE_FIELD, intent.getIntExtra(
						BatteryManager.EXTRA_VOLTAGE, 0));
				putValues(values, now, expire);
			}
		}

	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { TEMPERATURE_FIELD, LEVEL_FIELD, VOLTAGE_FIELD };
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
