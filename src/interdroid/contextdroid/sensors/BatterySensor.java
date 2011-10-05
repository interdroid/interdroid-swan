package interdroid.contextdroid.sensors;

import interdroid.vdb.content.avro.AvroContentProviderProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

public class BatterySensor extends AbstractAsynchronousSensor {

	public static final Logger LOG = LoggerFactory.getLogger(BatterySensor.class);

	public static final String LEVEL_FIELD = "level";
	public static final String VOLTAGE_FIELD = "voltage";
	public static final String TEMPERATURE_FIELD = "temperature";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

	public static final String SCHEME;

	public static class Provider extends AvroContentProviderProxy {

		public Provider() {
			super(SCHEME);
		}

	}

	static {
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
		SCHEME = scheme.replace('\'', '"');
		LOG.debug("Schema: {}", SCHEME);
	}

	private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				long now = System.currentTimeMillis();
				long expire = now + EXPIRE_TIME;

				ContentValues values = new ContentValues();
				values.put(LEVEL_FIELD, intent.getIntExtra(
					BatteryManager.EXTRA_LEVEL, 0));
				values.put(TEMPERATURE_FIELD,intent.getIntExtra(
					BatteryManager.EXTRA_TEMPERATURE, 0));
				values.put(VOLTAGE_FIELD, intent.getIntExtra(
					BatteryManager.EXTRA_VOLTAGE, 0));
				putValues(values, now, expire);
			}
		}

	};

	public void onDestroy() {
		unregisterReceiver(batteryReceiver);
		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { TEMPERATURE_FIELD, LEVEL_FIELD, VOLTAGE_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public void onConnected() {
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(batteryReceiver, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
		}
	}

	@Override
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(batteryReceiver);
		}
	}

}
