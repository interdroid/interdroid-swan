package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractMemorySensor;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class ProximitySensor extends AbstractMemorySensor {

	public static final String TAG = "ProximitySensor";

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
			return R.xml.proximity_preferences;
		}

	}

	/** Value of ACCURACY must be one of SensorManager.SENSOR_DELAY_* */
	public static final String ACCURACY = "accuracy";

	public static final String PROXIMITY_FIELD = "proximity";

	protected static final int HISTORY_SIZE = 30;

	private Sensor proximitySensor;
	private SensorManager sensorManager;
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (sensor.getType() == Sensor.TYPE_PROXIMITY) {
				currentConfiguration.putInt(ACCURACY, accuracy);
			}
		}

		public void onSensorChanged(SensorEvent event) {
			long now = System.currentTimeMillis();
			if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
				for (int i = 0; i < 3; i++) {
					putValueTrimSize(VALUE_PATHS[i], null, now,
							event.values[i], HISTORY_SIZE);
				}
			}
		}
	};

	@Override
	public String[] getValuePaths() {
		return new String[] { PROXIMITY_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putInt(ACCURACY,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'proximitySensor', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ PROXIMITY_FIELD
				+ "', 'type': 'double'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensorList = sensorManager
				.getSensorList(Sensor.TYPE_PROXIMITY);
		if (sensorList.size() > 0) {
			proximitySensor = sensorList.get(0);
		} else {
			Log.e(TAG, "No proximitySensor found on device!");
		}
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		updateAccuracy();
	}

	private void updateAccuracy() {
		sensorManager.unregisterListener(sensorEventListener);
		if (registeredConfigurations.size() > 0) {

			int highestAccuracy = mDefaultConfiguration.getInt(ACCURACY);
			for (Bundle configuration : registeredConfigurations.values()) {
				if (configuration == null) {
					continue;
				}
				if (configuration.containsKey(ACCURACY)) {
					highestAccuracy = Math
							.min(highestAccuracy,
									Integer.parseInt(configuration
											.getString(ACCURACY)));
				}
			}
			highestAccuracy = Math.max(highestAccuracy,
					SensorManager.SENSOR_DELAY_FASTEST);
			sensorManager.registerListener(sensorEventListener, proximitySensor,
					highestAccuracy);
		}

	}

	@Override
	public final void unregister(String id) {
		updateAccuracy();
	}

	@Override
	public final void onDestroySensor() {
		sensorManager.unregisterListener(sensorEventListener);
	}
}
