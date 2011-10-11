package interdroid.contextdroid.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;

public class BatterySensor extends AbstractAsynchronousSensor {

	public static final String TAG = "BatterySensor";

	public static final String LEVEL_FIELD = "level";
	public static final String VOLTAGE_FIELD = "voltage";
	public static final String TEMPERATURE_FIELD = "temperature";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

	private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
				long now = System.currentTimeMillis();
				long expire = now + EXPIRE_TIME;
				trimValues(HISTORY_SIZE);
				putValue(LEVEL_FIELD, now, expire,
						intent.getIntExtra(
								BatteryManager.EXTRA_LEVEL, 0));
				putValue(TEMPERATURE_FIELD, now, expire,
						intent.getIntExtra(
								BatteryManager.EXTRA_TEMPERATURE, 0));
				putValue(VOLTAGE_FIELD, now, expire,
						intent.getIntExtra(
								BatteryManager.EXTRA_VOLTAGE, 0));
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
		return "{'type': 'record', 'name': 'battery', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ LEVEL_FIELD
				+ "', 'type': 'integer'},"
				+ "            {'name': '"
				+ VOLTAGE_FIELD
				+ "', 'type': 'integer'},"
				+ "            {'name': '"
				+ TEMPERATURE_FIELD
				+ "', 'type': 'integer'}"
				+ "           ]"
				+ "}".replace('\'', '"');
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
