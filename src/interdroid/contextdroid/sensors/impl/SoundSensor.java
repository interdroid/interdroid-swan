package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractMemorySensor;

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
	public static class SoundConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.sound_preferences;
		}

	}

	public static final String DB_FIELD = "dB";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String SAMPLE_LENGTH = "sample_length";
	public static final String SAMPLE_RATE = "sample_rate";
	public static final String AUDIO_SOURCE = "audio_source";
	public static final String CHANNEL_CONFIG = "channel_config";
	public static final String AUDIO_FORMAT = "audio_format";

	public static final long DEFAULT_SAMPLE_INTERVAL = 10 * 1000;
	public static final int DEFAULT_SAMPLE_LENGTH = 1024;
	public static final int DEFAULT_SAMPLE_RATE = 8000;
	public static final int DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
	public static final int DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	public static final int DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

	protected static final int HISTORY_SIZE = 10;

	private Map<String, SoundPoller> activeThreads = new HashMap<String, SoundPoller>();

	/**
	 * Sample rms.
	 * 
	 * @return the RMS of the sample in dB
	 */
	private double sampleRms(AudioRecord audioRecord, int sampleLength) {
		short[] buffer = new short[sampleLength];
		int numshorts = 0;
		long sum_squares;
		double rmspow;

		// take 1024 samples
		audioRecord.startRecording();
		while (numshorts < sampleLength) {
			numshorts += audioRecord.read(buffer, numshorts, sampleLength
					- numshorts);
		}
		audioRecord.stop();

		// calculate power in dB (RMS)
		sum_squares = 0;
		for (int i = 0; i < numshorts; i++) {
			sum_squares += buffer[i] * buffer[i];
		}
		rmspow = 10.0 * Math.log10(Math.sqrt((double) sum_squares / numshorts)
				/ Short.MAX_VALUE);
		System.out.println(rmspow);
		return rmspow;
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { DB_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
		DEFAULT_CONFIGURATION.putInt(SAMPLE_LENGTH, DEFAULT_SAMPLE_LENGTH);
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
				+ DB_FIELD
				+ "', 'type': 'double'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		System.out.println("sound sensor connected!");
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		SoundPoller soundPoller = new SoundPoller(id, valuePath, configuration);
		activeThreads.put(id, soundPoller);
		soundPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class SoundPoller extends Thread {

		private String id;
		private Bundle configuration;
		private AudioRecord audioRecord;
		private int sampleLength;
		private String valuePath;

		SoundPoller(String id, String valuePath, Bundle configuration) {
			this.id = id;
			this.configuration = configuration;
			this.valuePath = valuePath;
			int buffersize = 8 * AudioRecord.getMinBufferSize(
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
					buffersize);
			sampleLength = configuration.getInt(SAMPLE_LENGTH,
					mDefaultConfiguration.getInt(SAMPLE_LENGTH));

		}

		public void run() {
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				putValueTrimSize(valuePath, id, start,
						sampleRms(audioRecord, sampleLength), HISTORY_SIZE);
				try {
					Thread.sleep(configuration.getLong(SAMPLE_INTERVAL,
							mDefaultConfiguration.getLong(SAMPLE_INTERVAL))
							+ start - System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}

	}

	@Override
	public void onDestroySensor() {
		for (SoundPoller soundPoller : activeThreads.values()) {
			soundPoller.interrupt();
		}
	};

}
