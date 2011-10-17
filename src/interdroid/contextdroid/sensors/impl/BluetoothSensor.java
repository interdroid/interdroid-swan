package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/**
 * A sensor for discovered bluetooth devices.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class BluetoothSensor extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(CallSensor.class);


	/**
	 * This configuration activity for this sensor.
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 *
	 */
	public static class ConfigurationActivity
	extends AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.bluetooth_preferences;
		}

	}


	/**
	 * The device name field.
	 */
	public static final String DEVICE_NAME_FIELD = "name";
	/**
	 * The device address field.
	 */
	public static final String DEVICE_ADDRESS_FIELD = "address";

	/**
	 * The bond state for the device.
	 */
	public static final String DEVICE_BOND_STATE = "bond_state";

	/**
	 * The class of the device.
	 */
	private static final String	DEVICE_CLASS	= "class";

	/**
	 * The major class of the device.
	 */
	private static final String	DEVICE_MAJOR_CLASS	= "major_class";

	/**
	 * The discovery interval.
	 */
	public static final String DISCOVERY_INTERVAL = "discovery_interval";

	/**
	 * The default expiration time.
	 */
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

	/**
	 * The default discovery interval.
	 */
	public static final long DEFAULT_DISCOVERY_INTERVAL = EXPIRE_TIME;


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

	/**
	 * @return the schema for this sensor.
	 */
	private static String getSchema() {
		String scheme =
				"{'type': 'record', 'name': 'bluetooth', "
						+ "'namespace': 'interdroid.context.sensor.bluetooth',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ DEVICE_NAME_FIELD
						+ "', 'type': 'string'},"
						+ "\n{'name': '"
						+ DEVICE_ADDRESS_FIELD
						+ "', 'type': 'string'},"
						+ "\n{'name': '"
						+ DEVICE_CLASS
						+ "', 'type': 'int'},"
						+ "\n{'name': '"
						+ DEVICE_MAJOR_CLASS
						+ "', 'type': 'int'}"
						+ "\n]"
						+ "}";
		return scheme.replace('\'', '"');
	}


	/**
	 * A flag to indicate we should stop polling.
	 */
	private boolean stopPolling = false;

	/**
	 * The bluetooth adapter we use to do discovery.
	 */
	private BluetoothAdapter mBluetoothAdapter;

	/**
	 * The broadcast receiver which gets information on bluetooth discovery.
	 */
	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			long now = System.currentTimeMillis();
			long expire = now + EXPIRE_TIME;
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			ContentValues values = new ContentValues();
			LOG.debug("Bluetooth discovered: {} {}",
					device.getName(), device.getAddress());
			values.put(DEVICE_NAME_FIELD, device.getName());
			values.put(DEVICE_ADDRESS_FIELD, device.getAddress());

			putValues(values, now, expire);
		}

	};

	/**
	 * The thread which polls for bluetooth devices.
	 */
	private Thread bluetoothPoller = new Thread() {
		public void run() {
			while (!stopPolling) {
				long start = System.currentTimeMillis();
				if (registeredConfigurations.size() > 0) {
					if (mBluetoothAdapter != null
							&& !mBluetoothAdapter.isDiscovering()) {
						mBluetoothAdapter.startDiscovery();
						LOG.debug("bt started discovery");
					}
				}
				try {
					Thread.sleep(currentConfiguration
							.getLong(DISCOVERY_INTERVAL)
							+ start
							- System.currentTimeMillis());
				} catch (InterruptedException e) {
					LOG.warn("Interrupted while sleeping.", e);
				}
			}
		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { DEVICE_ADDRESS_FIELD, DEVICE_NAME_FIELD,
				DEVICE_BOND_STATE, DEVICE_CLASS, DEVICE_MAJOR_CLASS};
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putLong(DISCOVERY_INTERVAL,
				DEFAULT_DISCOVERY_INTERVAL);
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		LOG.debug("bluetooth connected");
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(bluetoothReceiver, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));
			if (!bluetoothPoller.isAlive()) {
				bluetoothPoller.start();
			}
		}
		updatePollRate();
	}

	/**
	 * Update the polling rate.
	 */
	private void updatePollRate() {
		boolean keepDefault = true;
		long updatedPollRate = Long.MAX_VALUE;
		for (Bundle configuration : registeredConfigurations.values()) {
			if (configuration.containsKey(DISCOVERY_INTERVAL)) {
				keepDefault = false;
				updatedPollRate = Math.min(updatedPollRate,
						configuration.getLong(DISCOVERY_INTERVAL));
			}
		}
		if (keepDefault) {
			currentConfiguration.putLong(DISCOVERY_INTERVAL,
					DEFAULT_DISCOVERY_INTERVAL);
		} else {
			currentConfiguration.putLong(DISCOVERY_INTERVAL, updatedPollRate);
		}
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(bluetoothReceiver);
		}
		updatePollRate();
	}

	@Override
	public final void onDestroySensor() {
		unregisterReceiver(bluetoothReceiver);

		mBluetoothAdapter.cancelDiscovery();
		stopPolling = true;
		bluetoothPoller.interrupt();
	}

}
