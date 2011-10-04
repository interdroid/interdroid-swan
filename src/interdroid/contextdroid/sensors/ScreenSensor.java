package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class ScreenSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "ScreenSensor";

	public static final String IS_SCREEN_ON_FIELD = "is_screen_on";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

	private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			long now = System.currentTimeMillis();
			if (values.get(IS_SCREEN_ON_FIELD).size() >= HISTORY_SIZE) {
				values.get(IS_SCREEN_ON_FIELD).remove(0);
			}
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				values.get(IS_SCREEN_ON_FIELD).add(
						new TimestampedValue(false, now, now + EXPIRE_TIME));
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				values.get(IS_SCREEN_ON_FIELD).add(
						new TimestampedValue(true, now, now + EXPIRE_TIME));
			}
			notifyDataChanged(IS_SCREEN_ON_FIELD);
		}

	};

	public void onDestroy() {
		unregisterReceiver(screenReceiver);
		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { IS_SCREEN_ON_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'screen', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ IS_SCREEN_ON_FIELD
				+ "', 'type': 'boolean'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		System.out.println("screen sensor connected");
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			registerReceiver(screenReceiver, filter);
		}
	}

	@Override
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(screenReceiver);
		}
	}

	@Override
	protected List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
