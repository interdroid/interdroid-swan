package interdroid.swan.sensors;

import interdroid.cuckoo.base.NoResourceAvailableException;
import interdroid.cuckoo.client.Cuckoo;
import interdroid.cuckoo.client.Cuckoo.Resource;
import interdroid.cuckoo.client.Oracle;
import interdroid.swan.cuckoo_sensors.CuckooPoller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public abstract class AbstractCuckooSensor extends AbstractVdbSensor {

	private final static String REGISTRATION_ID = "registration_id";

	private Map<String, MonitorThread> monitors = new HashMap<String, MonitorThread>();

	private Resource remoteResource = null;

	private SharedPreferences prefs;

	@Override
	public final synchronized void onConnected() {
		// TODO improve picking of resource!
		List<Resource> resources = Oracle.getAllResources(this);
		System.out.println("got resources: " + resources.size());
		if (resources.size() > 0) {
			remoteResource = resources.get(0);
			prefs = getSharedPreferences(getClass().getSimpleName(),
					Context.MODE_PRIVATE);
			if (prefs.getString(REGISTRATION_ID, null) == null) {
				registerBackground();
			} else {
				System.out.println("regId: "
						+ prefs.getString(REGISTRATION_ID, null));
			}

			// create a gcm object?
		}
	}

	@Override
	public final void onDestroySensor() {
		remoteResource = null;
	}

	@Override
	public final synchronized void register(final String id,
			final String valuePath, Bundle configuration) throws IOException {
		final Map<String, Object> configAsMap = new HashMap<String, Object>();
		for (String key : configuration.keySet()) {
			// TODO: maybe short circuit if a value is not serializable
			configAsMap.put(key, configuration.get(key));
		}
		if (remoteResource != null) {
			System.out.println("Trying to register expression");
			new Thread() {
				public void run() {
					while (prefs.getString(REGISTRATION_ID, null) == null) {
						System.out.println("waiting for reg id");
						try {
							sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
					try {
						Cuckoo.register(AbstractCuckooSensor.this,
								remoteResource,
								prefs.getString(REGISTRATION_ID, null),
								getGCMApiKey(), getPoller(), id, valuePath,
								configAsMap);
						registerReceiver();

						return;
					} catch (NoResourceAvailableException e) {
						// fall back to local impl
						remoteResource = null;
					}
					// local
					System.out.println("EEP, local");
					MonitorThread monitor = new MonitorThread(
							AbstractCuckooSensor.this, valuePath, configAsMap);
					monitors.put(id, monitor);
					monitor.start();
				}
			}.start();

		}
	}

	@Override
	public final void unregister(String id) {
		if (remoteResource != null) {
			try {
				Cuckoo.unregister(this, remoteResource, id);
				return;
			} catch (NoResourceAvailableException e) {
				// TODO: we lost connection to this resource, retry later with
				// an alarm or so.

				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			monitors.remove(id).interrupt();
		}
	}

	public abstract CuckooPoller getPoller();

	public abstract String getGCMSenderId();

	public abstract String getGCMApiKey();

	public abstract void registerReceiver();

	private void registerBackground() {
		new Thread() {
			public void run() {
				try {
					System.out
							.println("getting a registration id for sender id: "
									+ getGCMSenderId());
					prefs.edit()
							.putString(
									REGISTRATION_ID,
									GoogleCloudMessaging.getInstance(
											AbstractCuckooSensor.this)
											.register(getGCMSenderId()))
							.commit();
					System.out.println("We did get a registration ID: "
							+ prefs.getString(REGISTRATION_ID, null));
				} catch (IOException e) {
					// TODO: something useful.
				}
			}
		}.start();
	}

	public static Object makeTyped(String string) {
		String[] components = string.split(":", 2);
		String type = components[0];
		String value = components[1];

		if (type.equals("java.lang.Long")) {
			return Long.parseLong(value);
		} else if (type.equals("java.lang.String")) {
			return value;
		} else if (type.equals("java.lang.Integer")) {
			return Integer.parseInt(value);
		} else if (type.equals("java.lang.Boolean")) {
			return Boolean.parseBoolean(value);
		} else if (type.equals("java.lang.Character")) {
			return value.charAt(0);
		} else if (type.equals("java.lang.Float")) {
			return Float.parseFloat(value);
		} else if (type.equals("java.lang.Double")) {
			return Double.parseDouble(value);
		} else {
			throw new RuntimeException("Please implement "
					+ AbstractCuckooSensor.class
					+ " makeTyped(String string) for type '" + type + "'");
		}
	}
}