package interdroid.swan.sensors.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.os.Bundle;
import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

/**
 * This sensor knows how to interface to a Bluetooth Polar Heart Rate Monitor.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class PolarHeartRate extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(PolarHeartRate.class);


	/**
	 * This configuration activity for this sensor.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static class ConfigurationActivity
	extends AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.polar_hrm_preferences;
		}

	}

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

	/** Beats per minute. */
	public static final String BPM = "bpm";
	/** Status. I think this is battery. */
	public static final String STATUS = "status";
	/** This is some sort of index: 1-15. Is this time related? */
	public static final String INDEX = "index";
	/** RRI values in ms. 60000 / rri = bpm. */
	public static final String RRI = "rri";
	/** Status text, shows the status of the connection to the sensor */
	public static final String STATUS_TEXT_FIELD = "status_text";

	/** The device name configuration. */
	public static final String DEVICE_NAME = "deviceName";
	/** The default device name to look for. */
	private static final String	DEFAULT_DEVICE_NAME	= "Polar iWL";

	/**
	 * @return the schema for this sensor.
	 */
	private static String getSchema() {
		String scheme =
				"{'type': 'record', 'name': 'polar_hrm', "
						+ "'namespace': 'interdroid.context.sensor.polar_hrm',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ BPM
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ INDEX
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ STATUS
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ STATUS_TEXT_FIELD
						+ "', 'type': 'string'},"
						+ "\n{'name': '"
						+ RRI
						+ "', 'type': 'int'}"
						+ "\n]"
						+ "}";
		return scheme.replace('\'', '"');
	}

	/** The bluetooth adapter we use to get access to bluetooth. */
	private BluetoothAdapter	mBluetoothAdapter;
	/** The threads which are monitoring various devices. */
	private Map<String, Thread> mServiceThreads = new HashMap<String, Thread>();

	/** A thread monitoring one device. */
	private class DeviceThread extends Thread {
		/** Time to sleep before pooling for more input to read. */
		private static final int	POLL_DELAY	= 100;
		/** Maximum byte value */
		private static final int	MAX_BYTE	= 256;
		/** Size of the protocol header. */
		private static final int	HEADER_SIZE	= 7;
		/** The maximum check value. */
		private static final int	CHECK_MAX	= 255;
		/** The start of a frame. */
		private static final int	FRAME_HEADER_MAGIC	= 254;

		/** The input stream from the device. */
		private InputStream mStream;

		/**
		 * Construct a DeviceThread to monitor the stream.
		 *
		 * @param stream the stream to monitor
		 */
		public DeviceThread(final InputStream stream) {
			mStream = stream;
		}

		@Override
		public void run() {
			try {
				LOG.debug("Built input stream.");
				boolean interrupted = false;

				// Avoid pressure on the GC
				ContentValues values = new ContentValues();
				List<String> rris = new ArrayList<String>();
				while (!interrupted && !interrupted()) {
					if (mStream.available() > 0) {
						int header = -1;
						// Ensure we have the header byte
						while (header != FRAME_HEADER_MAGIC) {
							header = mStream.read();
						}
						int size = mStream.read();
						int check = mStream.read();
						if (check != (CHECK_MAX - size)) {
							LOG.debug("Bad packet. Skipping.");
							continue;
						}
						// Index runs from 1 to 15 and repeats
						int index = mStream.read();
						// Always seeing 241 for status? Battery?
						int status = mStream.read();
						// This seems to be right.
						int bpm = mStream.read();

						// Clear the array
						rris.clear();
						for (int i = HEADER_SIZE; i < size; i += 2) {
							rris.add(String.valueOf(mStream.read() * MAX_BYTE
									+ mStream.read()));
						}
						storeValues(values, index, status, bpm, rris);
					} else {
						try {
							sleep(POLL_DELAY);
						} catch (InterruptedException e) {
							interrupted = true;
							LOG.debug(
									"Interrupted while waiting for reading: {}",
									interrupted());
						}
					}
				}


			} catch (IOException e) {
				LOG.error("Error creating socket.", e);
			} finally {
				LOG.debug("Closing bluetooth socket.");
				if (mStream != null) {
					try {
						mStream.close();
					} catch (Exception e) {
						LOG.error("Error closing input stream", e);
					}
				}
			}

		}

	};

	/**
	 * Store values to the DB
	 * @param values for reuse
	 * @param index the index
	 * @param status the status
	 * @param bpm the bpm
	 * @param rris list of rri values
	 */
	private void storeValues(final ContentValues values, final int index,
			final int status, final int bpm, final List<String> rris) {

		long now = System.currentTimeMillis();

		for (String rri : rris) {
			// Clear the values
			values.clear();

			// Store the data.
			values.put(BPM, bpm);
			values.put(INDEX, index);
			values.put(STATUS, status);
			values.put(RRI, rri);
			values.put(STATUS_TEXT_FIELD, statusAsText(status));

			// Send it to the database
			putValues(values, now);
		}

	}
	
	private String statusAsText(int status){
		switch(status){
		case 241:
			return "Connected - Active reading HR";
		case 209:
			return "Connected - Active reading HR";
		case 251:
			return "Connected - Problem reading HR";
		case 193:
			return "Disconnected";
		default:
			return "Unknown: " + status;
		}
		}
	

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) throws IOException {
		if (!mBluetoothAdapter.isEnabled()) {
			throw new IllegalStateException("Bluetooth is not enabled.");
		}

		String deviceName = configuration.getString(DEVICE_NAME);
		if (null == deviceName) {
			deviceName = mDefaultConfiguration.getString(DEVICE_NAME);
		}
		boolean found = false;
		if (mBluetoothAdapter.isEnabled()) {
			for (BluetoothDevice device
				: mBluetoothAdapter.getBondedDevices()) {
				if (device.getName().equals(deviceName)) {
					LOG.debug("Found paired Polar iWL {}" + device);

					UUID uuid = UUID.fromString(
							"00001101-0000-1000-8000-00805F9B34FB");
					BluetoothSocket socket;
					try {
						socket = device.createRfcommSocketToServiceRecord(uuid);
						socket.connect();
						InputStream inStream = socket.getInputStream();

						DeviceThread serviceThread = new DeviceThread(inStream);
						serviceThread.start();
						mServiceThreads.put(id, serviceThread);
						found = true;
						break;
					} catch (IOException e) {
						LOG.error("Unable to connect to device.", e);
						throw e;
					}
				}
			}
		}
		if (!found) {
			throw new IllegalArgumentException(
					"Unable to find bonded device to pair with name: "
			+ deviceName);
		}
	}

	@Override
	public final void unregister(final String id) {
		Thread thread = mServiceThreads.get(id);
		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onDestroySensor() {
		for (Thread thread : mServiceThreads.values()) {
			thread.interrupt();
		}

	}

	@Override
	public final void onConnected() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putString(DEVICE_NAME, DEFAULT_DEVICE_NAME);
	}

	@Override
	public final String[] getValuePaths() {
		return new String[] { BPM };
	}

}
