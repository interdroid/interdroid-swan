package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractMemorySensor;

import java.util.HashMap;
import java.util.Map;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;

public class SoundSensor extends AbstractMemorySensor {

	public static final String TAG = "Sound";

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
			return R.xml.sound_preferences;
		}

	}

	public static final String RMS_FIELD = "rms";
	public static final String DB_FIELD = "db";

	public static final String PEAK_DB = "peak_db";
	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String SAMPLE_RATE = "sample_rate";
	public static final String AUDIO_SOURCE = "audio_source";
	public static final String CHANNEL_CONFIG = "channel_config";
	public static final String AUDIO_FORMAT = "audio_format";

	public static final int DEFAULT_PEAK_DB = 70;
	public static final int DEFAULT_SAMPLE_INTERVAL = 10 * 1000;
	public static final int DEFAULT_SAMPLE_RATE = 8000;
	public static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
	public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

	protected static final int HISTORY_SIZE = 3000;

	private Map<String, SoundPoller> activeThreads = new HashMap<String, SoundPoller>();

	@Override
	public String[] getValuePaths() {
		return new String[] { RMS_FIELD, DB_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putDouble(PEAK_DB, DEFAULT_PEAK_DB);
		DEFAULT_CONFIGURATION.putInt(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
		DEFAULT_CONFIGURATION.putInt(SAMPLE_RATE, DEFAULT_SAMPLE_RATE);
		DEFAULT_CONFIGURATION.putInt(AUDIO_SOURCE, DEFAULT_AUDIO_SOURCE);
		DEFAULT_CONFIGURATION.putInt(AUDIO_FORMAT, DEFAULT_AUDIO_FORMAT);
		DEFAULT_CONFIGURATION.putInt(CHANNEL_CONFIG, DEFAULT_CHANNEL_CONFIG);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'sound', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ RMS_FIELD
				+ "', 'type': 'double'}, "
				+ "            {'name': '"
				+ DB_FIELD
				+ "', 'type': 'double'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		SoundPoller soundPoller = new SoundPoller(id, valuePath, configuration);
		activeThreads.put(id, soundPoller);
		soundPoller.start();
	}

	@Override
	public final void unregister(String id) {
		System.out.println("UNREGISTER SOUND SENSOR!");
		activeThreads.remove(id).shouldStop();
	}

	class SoundPoller extends Thread {

		private String id;
		private Bundle configuration;
		private AudioRecord audioRecord;
		private String valuePath;
		private int bufferSize;
		private double peakDb;
		private boolean shouldStop = false;

		SoundPoller(String id, String valuePath, Bundle configuration) {
			this.id = id;
			this.configuration = configuration;
			this.valuePath = valuePath;
			this.bufferSize = 8 * AudioRecord.getMinBufferSize(
					configuration.getInt(SAMPLE_RATE,
							mDefaultConfiguration.getInt(SAMPLE_RATE)),
					configuration.getInt(CHANNEL_CONFIG,
							mDefaultConfiguration.getInt(CHANNEL_CONFIG)),
					configuration.getInt(AUDIO_FORMAT,
							mDefaultConfiguration.getInt(AUDIO_FORMAT)));
			audioRecord = new AudioRecord(configuration.getInt(AUDIO_SOURCE,
					mDefaultConfiguration.getInt(AUDIO_SOURCE)),
					configuration.getInt(SAMPLE_RATE,
							mDefaultConfiguration.getInt(SAMPLE_RATE)),
					configuration.getInt(CHANNEL_CONFIG,
							mDefaultConfiguration.getInt(CHANNEL_CONFIG)),
					configuration.getInt(AUDIO_FORMAT,
							mDefaultConfiguration.getInt(AUDIO_FORMAT)),
					bufferSize);
			peakDb = configuration.getDouble(PEAK_DB,
					mDefaultConfiguration.getDouble(PEAK_DB));

		}

		private void shouldStop() {
			shouldStop = true;
		}

		public void run() {
			while (!shouldStop) {
				long start = System.currentTimeMillis();
				double sample = sample(audioRecord, bufferSize,
						DB_FIELD.equals(valuePath) ? peakDb : -1);
				System.out.println(valuePath + ": " + sample);
				putValueTrimSize(valuePath, id, start, sample, HISTORY_SIZE);
				long sleepTime = configuration.getInt(SAMPLE_INTERVAL,
						mDefaultConfiguration.getInt(SAMPLE_INTERVAL))
						+ start
						- System.currentTimeMillis();
				if (sleepTime > 0) {
					try {
						sleep(sleepTime);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		/**
		 * Sample rms.
		 * 
		 * @return the RMS of the sample
		 */
		private double sample(AudioRecord audioRecord, int sampleLength,
				double peakDb) {
			short[] buffer = new short[sampleLength];
			int position = 0;

			// take the samples
			audioRecord.startRecording();
			while (position < sampleLength) {
				position += audioRecord.read(buffer, position, sampleLength
						- position);
			}
			audioRecord.stop();

			double sumOfSquares = 0;
			for (int i = 0; i < sampleLength; i++) {
				sumOfSquares += buffer[i] * buffer[i];
			}

			double rms = Math.sqrt(sumOfSquares / sampleLength);
			if (peakDb < 0) {
				return rms;
			}
			double db = peakDb + 20 * Math.log10(rms / Short.MAX_VALUE);
			return db;
		}

	}

	@Override
	public void onDestroySensor() {
		for (SoundPoller soundPoller : activeThreads.values()) {
			soundPoller.interrupt();
		}
	};

}
