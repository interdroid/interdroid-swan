package interdroid.contextdroid.sensors;

import interdroid.vdb.content.avro.AvroContentProviderProxy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

/**
 * A sensor for available Wifi networks.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class WifiSensor extends AbstractAsynchronousSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(WifiSensor.class);

	/**
	 * The network identifier field.
	 */
	public static final String SSID_FIELD = "ssid";
	/**
	 * The base station identifier field.
	 */
	public static final String BSSID_FIELD = "bssid";
	/**
	 * The level seen.
	 */
	public static final String LEVEL_FIELD = "level";

	/**
	 * The discovery interval.
	 */
	public static final String DISCOVERY_INTERVAL = "discovery_interval";

	/**
	 * The default expiration time.
	 */
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

	/**
	 * The interval at which to run discovery.
	 */
	public static final long DEFAULT_DISCOVERY_INTERVAL = 30 * 1000;

	/**
	 * true if we should stop polling and shutdown.
	 */
	private boolean stopPolling = false;

	/**
	 * The wifi manager we access wifi info with.
	 */
	private WifiManager wifiManager;

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
				"{'type': 'record', 'name': 'wifi', "
						+ "'namespace': 'interdroid.context.sensor.wifi',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ SSID_FIELD
						+ "', 'type': 'string'},"
						+ "\n{'name': '"
						+ BSSID_FIELD
						+ "', 'type': 'string'},"
						+ "\n{'name': '"
						+ LEVEL_FIELD
						+ "', 'type': 'int'}"
						+ "\n]"
						+ "}";
		return scheme.replace('\'', '"');
	}


	/**
	 * The receiver we use to get wifi notifications.
	 */
	private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			long now = System.currentTimeMillis();
			long expire = now + EXPIRE_TIME;

			List<ScanResult> results = wifiManager.getScanResults();
			for (ScanResult scanResult : results) {
				LOG.debug("Got WiFi: {} {} " + scanResult.level,
						scanResult.SSID, scanResult.BSSID);
				ContentValues values = new ContentValues();

				values.put(SSID_FIELD, scanResult.SSID);
				values.put(BSSID_FIELD, scanResult.BSSID);
				values.put(LEVEL_FIELD, scanResult.level);
				putValues(values, now, expire);
			}
		}

	};

	/**
	 * The thread we use to poll wifi.
	 */
	private Thread wifiPoller = new Thread() {
		public void run() {
			while (!stopPolling) {
				long start = System.currentTimeMillis();
				if (registeredConfigurations.size() > 0) {
					LOG.debug("Starting WiFi scan.");
					wifiManager.startScan();
				}
				try {
					LOG.debug("Sleeping.");
					synchronized (wifiPoller) {
						wifiPoller.wait(currentConfiguration
								.getLong(DISCOVERY_INTERVAL)
								+ start
								- System.currentTimeMillis());
					}
				} catch (InterruptedException e) {
					LOG.error("Interrupted while waiting.", e);
				}
			}
		}
	};

	@Override
	public final void onDestroy() {
		try {
			unregisterReceiver(wifiReceiver);
		} catch (IllegalArgumentException e) {
			LOG.error("Error unregistering", e);
		}

		stopPolling = true;
		wifiPoller.interrupt();

		super.onDestroy();
	}

	@Override
	public final String[] getValuePaths() {
		return new String[] { SSID_FIELD, BSSID_FIELD, LEVEL_FIELD};
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putLong(DISCOVERY_INTERVAL, DEFAULT_DISCOVERY_INTERVAL);
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	protected final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(wifiReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			if (!wifiPoller.isAlive()) {
				wifiPoller.start();
			} else {
				synchronized (wifiPoller) {
					wifiPoller.notifyAll();
				}
			}
		}
		updatePollRate();
	}

	/**
	 * Updates the polling rate when we get a new registration.
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
	protected final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(wifiReceiver);
		}
		updatePollRate();
	}

}
