package interdroid.contextdroid.sensors;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class BluetoothSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "Bluetooth";

	public static final String DEVICE_NAME_FIELD = "name";
	public static final String DEVICE_ADDRESS_FIELD = "address";
	public static final String DEVICE_BUNDLED_FIELD = "bundled";

	public static final String DISCOVERY_INTERVAL = "discovery_interval";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?
	public static final long DEFAULT_DISCOVERY_INTERVAL = 5 * 60 * 1000;

	private boolean stopPolling = false;

	private BluetoothAdapter mBluetoothAdapter;

	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			long now = System.currentTimeMillis();
			long expire = now + EXPIRE_TIME;
			trimValues(HISTORY_SIZE);
			BluetoothDevice device = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			putValue(DEVICE_NAME_FIELD, now, expire, device.getName());
			putValue(DEVICE_ADDRESS_FIELD, now, expire, device.getAddress());
			Bundle bothBundle = new Bundle();
			bothBundle.putString("name", device.getName());
			bothBundle.putString("address", device.getAddress());
			putValue(DEVICE_BUNDLED_FIELD, now, expire, bothBundle);

			System.out.println("bt found: " + device.getName());
		}

	};

	private Thread bluetoothPoller = new Thread() {
		public void run() {
			while (!stopPolling) {
				long start = System.currentTimeMillis();
				if (registeredConfigurations.size() > 0) {
					if (mBluetoothAdapter != null
							&& !mBluetoothAdapter.isDiscovering()) {
						mBluetoothAdapter.startDiscovery();
						System.out.println("bt started discovery");
					}
				}
				try {
					Thread.sleep(currentConfiguration
							.getLong(DISCOVERY_INTERVAL)
							+ start
							- System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}
	};

	public void onDestroy() {
		unregisterReceiver(bluetoothReceiver);

		mBluetoothAdapter.cancelDiscovery();
		stopPolling = true;
		bluetoothPoller.interrupt();

		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { DEVICE_ADDRESS_FIELD, DEVICE_NAME_FIELD,
				DEVICE_BUNDLED_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(DISCOVERY_INTERVAL,
				DEFAULT_DISCOVERY_INTERVAL);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'bluetooth', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ DEVICE_NAME_FIELD
				+ "', 'type': 'string'},"
				+ "            {'name': '"
				+ DEVICE_ADDRESS_FIELD
				+ "', 'type': 'string'},"
				+ "            {'name': '"
				+ DEVICE_BUNDLED_FIELD
				+ "', 'type': 'bytes'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		System.out.println("bluetooth connected");
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(bluetoothReceiver, new IntentFilter(
					BluetoothDevice.ACTION_FOUND));
			if (!bluetoothPoller.isAlive()) {
				bluetoothPoller.start();
			}
		}
		updatePollRate();
	}

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
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(bluetoothReceiver);
		}
		updatePollRate();
	}

}
