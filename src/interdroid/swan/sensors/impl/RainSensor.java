package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractMemorySensor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;

/**
 * Based on the original WeatherSensor written by Rick de Leeuw
 * 
 * @author rkemp
 */
public class RainSensor extends AbstractMemorySensor {

	public static final String TAG = "Rain";

	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.rain_preferences;
		}

	}

	public static final String START_TIME_FIELD = "start_time";
	public static final String STOP_TIME_FIELD = "stop_time";
	public static final String EXPECTED_MM_FIELD = "expected_mm";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String THRESHOLD = "threshold";
	public static final String WINDOW = "window";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;
	public static final long DEFAULT_WINDOW = 2 * 60 * 60 * 1000; // 2 hours
	public static final int DEFAULT_THRESHOLD = 0; // 0mm

	protected static final int HISTORY_SIZE = 10;

	// Buienradar specific variables
	private static final String BASE_URL = "http://gps.buienradar.nl/getrr.php?lat=%s&lon=%s";
	private static final int SAMPLE_LENGTH = 5 * 60 * 1000;

	private Map<String, RainPoller> activeThreads = new HashMap<String, RainPoller>();

	private float sampleRain(String url, long window) {
		float result = 0;
		int nrSamples = (int) (window / SAMPLE_LENGTH);
		float fractionOfLastSample = ((window % SAMPLE_LENGTH) / (float) SAMPLE_LENGTH);

		try {
			URLConnection conn = new URL(url).openConnection();

			BufferedReader r = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			for (int i = 0; i < nrSamples; i++) {
				String line = r.readLine();
				if (line == null) {
					break;
				} else {
					Date date = new Date();
					date.setHours(Integer.parseInt(line.substring(4, 6)));
					date.setMinutes(Integer.parseInt(line.substring(7, 9)));
					date.setSeconds(0);
					if (date.after(new Date())) {
						result += convertValueToMM(
								Integer.parseInt(line.substring(0, 3)), 5);
					} else {
						nrSamples++;
					}
				}
			}
			String line = r.readLine();
			if (line != null) {
				Date date = new Date();
				date.setHours(Integer.parseInt(line.substring(4, 6)));
				date.setMinutes(Integer.parseInt(line.substring(7, 9)));
				date.setSeconds(0);
				if (date.after(new Date())) {
					result += (fractionOfLastSample * convertValueToMM(
							Integer.parseInt(line.substring(0, 3)), 5));
				}
			}
			r.close();
		} catch (Exception e) {
			// ignore
		}
		return result;
	}

	private float convertValueToMM(int value, int minutes) {
		float result = (float) ((Math.pow(10, (value - 109) / 32.0) / 60.0) * minutes);
		return result;
	}

	private Date sampleRain(String url, long window, int threshold,
			boolean lookForStart) {
		int nrSamples = (int) (window / SAMPLE_LENGTH);
		float fractionOfLastSample = ((window % SAMPLE_LENGTH) / (float) SAMPLE_LENGTH);

		try {
			URLConnection conn = new URL(url).openConnection();

			BufferedReader r = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			for (int i = 0; i < nrSamples; i++) {
				String line = r.readLine();
				if (line == null) {
					break;
				} else {
					boolean isRaining = convertValueToMM(
							Integer.parseInt(line.substring(0, 3)), 60) > threshold;
					if ((lookForStart && isRaining)
							|| (!lookForStart && !isRaining)) {
						Date result = new Date();
						result.setHours(Integer.parseInt(line.substring(4, 6)));
						result.setMinutes(Integer.parseInt(line.substring(7, 9)));
						result.setSeconds(0);
						if (result.after(new Date())) {
							r.close();
							return result;
						}
					}
				}
			}
			String line = r.readLine();
			if (line != null) {
				boolean isRaining = (int) (fractionOfLastSample * convertValueToMM(
						Integer.parseInt(line.substring(0, 3)), 60)) > threshold;
				if ((lookForStart && isRaining)
						|| (!lookForStart && !isRaining)) {
					Date result = new Date();
					result.setHours(Integer.parseInt(line.substring(4, 6)));
					result.setMinutes(Integer.parseInt(line.substring(7, 9)));
					result.setSeconds(0);
					if (result.after(new Date())) {
						r.close();
						return result;
					}
				}
			}
		} catch (Exception e) {
			// ignore
		}

		return null;
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { START_TIME_FIELD, STOP_TIME_FIELD,
				EXPECTED_MM_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
		DEFAULT_CONFIGURATION.putLong(WINDOW, DEFAULT_WINDOW);
		DEFAULT_CONFIGURATION.putInt(THRESHOLD, DEFAULT_THRESHOLD);
	}

//	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'rain', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ EXPECTED_MM_FIELD
				+ "', 'type': 'integer'},"
				+ "            {'name': '"
				+ START_TIME_FIELD
				+ "', 'type': 'date'},"
				+ "            {'name': '"
				+ STOP_TIME_FIELD
				+ "', 'type': 'date'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		RainPoller rainPoller = new RainPoller(id, valuePath, configuration);
		activeThreads.put(id, rainPoller);
		rainPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class RainPoller extends Thread {

		private String id;
		private Bundle configuration;
		private String valuePath;

		RainPoller(String id, String valuePath, Bundle configuration) {
			this.id = id;
			this.configuration = configuration;
			this.valuePath = valuePath;
		}

		public void run() {
			String url = String.format(BASE_URL, configuration.get(LATITUDE),
					configuration.get(LONGITUDE));
			long window = configuration.getLong(WINDOW,
					mDefaultConfiguration.getLong(WINDOW));
			int threshold = configuration.getInt(THRESHOLD,
					mDefaultConfiguration.getInt(THRESHOLD));
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				if (START_TIME_FIELD.equals(valuePath)) {
					Date date = sampleRain(url, window, threshold, true);
					if (date != null) {
						putValueTrimSize(valuePath, id, start, date,
								HISTORY_SIZE);
					}
				} else if (STOP_TIME_FIELD.equals(valuePath)) {
					Date date = sampleRain(url, window, threshold, false);
					if (date != null) {
						putValueTrimSize(valuePath, id, start, date,
								HISTORY_SIZE);
					}
				} else if (EXPECTED_MM_FIELD.equals(valuePath)) {
					float mm = sampleRain(url, window);
					putValueTrimSize(valuePath, id, start, mm, HISTORY_SIZE);
				}

				try {
					Thread.sleep(Math.max(
							0,
							configuration.getLong(SAMPLE_INTERVAL,
									mDefaultConfiguration
											.getLong(SAMPLE_INTERVAL))
									+ start - System.currentTimeMillis()));
				} catch (InterruptedException e) {
				}
			}
		}

	}

	@Override
	public void onDestroySensor() {
		for (RainPoller rainPoller : activeThreads.values()) {
			rainPoller.interrupt();
		}
	};

}
