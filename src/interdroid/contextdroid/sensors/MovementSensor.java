package interdroid.contextdroid.sensors;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

public class MovementSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "MovementSensor";

	/** Value of ACCURACY must be one of SensorManager.SENSOR_DELAY_* */
	public static final String ACCURACY = "accuracy";

	public static final String X_FIELD = "x";
	public static final String Y_FIELD = "y";
	public static final String Z_FIELD = "z";
	public static final String TOTAL_FIELD = "total";

	protected static final int HISTORY_SIZE = 30;

	private Sensor accelerometer;
	private SensorManager sensorManager;
	private SensorEventListener sensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				currentConfiguration.putInt(ACCURACY, accuracy);
			}
		}

		public void onSensorChanged(SensorEvent event) {
			long now = System.currentTimeMillis();
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				long expire = now + 100;
				trimValues(HISTORY_SIZE);
				for (int i = 0; i < 3; i++) {
					putValue(VALUE_PATHS[i], now, expire, event.values[i]);
				}
				float len2 = (float) Math.sqrt(event.values[0]
						* event.values[0] + event.values[1] * event.values[1]
						+ event.values[2] * event.values[2]);
				putValue(TOTAL_FIELD, now, expire, len2);

			}
		}
	};

	public void onDestroy() {
		sensorManager.unregisterListener(sensorEventListener);
		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { X_FIELD, Y_FIELD, Z_FIELD, TOTAL_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putInt(ACCURACY,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'movement', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ X_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ Y_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ Z_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ TOTAL_FIELD
				+ "', 'type': 'double'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensorList = sensorManager
				.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensorList.size() > 0) {
			accelerometer = sensorList.get(0);
		} else {
			Log.e(TAG, "No accelerometer found on device!");
		}
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		updateAccuracy();
	}

	private void updateAccuracy() {
		sensorManager.unregisterListener(sensorEventListener);
		if (registeredConfigurations.size() > 0) {

			int highestAccuracy = DEFAULT_CONFIGURATION.getInt(ACCURACY);
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
			sensorManager.registerListener(sensorEventListener, accelerometer,
					highestAccuracy);
		}

	}

	@Override
	protected void unregister(String id) {
		updateAccuracy();
	}
}
