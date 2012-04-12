package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractMemorySensor;
import interdroid.cuckoo.client.Cuckoo;

import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class CuckooTrainSensor extends AbstractMemorySensor {

	public static final String TAG = "CuckooTrain";

	/**
	 * The configuration activity for this class.
	 * 
	 * @author nick &lt;rkemp@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.cuckoo_train_preferences;
		}

	}

	public static final String DEPARTURE_TIME_FIELD = "departure_time";
	public static final String ARRIVAL_TIME_FIELD = "arrival_time";

	public static final String FROM_STATION = "from_station";
	public static final String TO_STATION = "to_station";
	public static final String DEPARTURE_TIME = "departure_time";

	protected static final int HISTORY_LENGTH = 24 * 60 * 60 * 1000; // one day

	BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String[] pushMessageItems = new String(
					intent.getByteArrayExtra("pushMessage")).split(":", 3);
			Date date = new Date(Long.parseLong(pushMessageItems[0]));
			String id = pushMessageItems[1];
			String valuePath = pushMessageItems[2];
			putValueTrimTime(valuePath, id, System.currentTimeMillis(), date,
					HISTORY_LENGTH);
		}
	};

	@Override
	public String[] getValuePaths() {
		return new String[] { DEPARTURE_TIME_FIELD, ARRIVAL_TIME_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'cuckootrain', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ DEPARTURE_TIME_FIELD
				+ "', 'type': 'long'},"
				+ "            {'name': '"
				+ ARRIVAL_TIME_FIELD
				+ "', 'type': 'long'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(
					mReceiver,
					new IntentFilter(
							"interdroid.swan.sensors.impl.CuckooTrainSensor.ACTION"));
		}

		try {
			Cuckoo.invokeMethodBind(this, id,
					"interdroid.swan.sensors.impl.CuckooTrainSensor",
					"start", new Object[] { id, valuePath, configuration });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public final void unregister(String id) {
		try {
			Cuckoo.invokeMethodUnbind(this, id,
					"interdroid.swan.sensors.impl.CuckooWebsiteSensor",
					"stop", new Object[] { id });
		} catch (Exception e) {
			e.printStackTrace();
		}

		// eventually unregister receiver
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(mReceiver);
		}
	}

	@Override
	public void onDestroySensor() {
		for (List<String> ids : this.expressionIdsPerValuePath.values()) {
			for (String id : ids) {
				unregister(id);
			}
		}
	};

}
