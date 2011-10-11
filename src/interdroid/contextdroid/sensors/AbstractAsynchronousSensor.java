package interdroid.contextdroid.sensors;

import interdroid.contextdroid.ConnectionListener;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Abstract class that implements basic functionality for sensors. Descendants
 * only have to implement requestReading() and onEntityServiceLevelChange(). The
 * rest can be overridden optionally.
 */
public abstract class AbstractAsynchronousSensor extends Service {

	protected String SCHEMA;

	protected String[] VALUE_PATHS;

	public final Bundle DEFAULT_CONFIGURATION = new Bundle();

	protected Bundle currentConfiguration = new Bundle();

	private Map<String, List<TimestampedValue>> values = new HashMap<String, List<TimestampedValue>>();

	protected Map<String, Bundle> registeredConfigurations = new HashMap<String, Bundle>();

	protected Map<String, String> registeredValuePaths = new HashMap<String, String>();

	protected Map<String, List<String>> expressionIdsPerValuePath = new HashMap<String, List<String>>();

	/** The context manager. */
	protected SensorContextServiceConnector contextServiceConnector;

	private HashMap<String, Boolean> notified = new HashMap<String, Boolean>();

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onBind(android.content.Intent)
	 *
	 * returns the sensor interface
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onCreate()
	 *
	 * Creates the ContextManager and connects to the ContextDroid service.
	 */
	@Override
	public void onCreate() {
		contextServiceConnector = new SensorContextServiceConnector(this);
		contextServiceConnector.start(new ConnectionListener() {

			@Override
			public void onDisconnected() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConnected() {
				AbstractAsynchronousSensor.this.onConnected();
				synchronized (contextServiceConnector) {
					contextServiceConnector.notifyAll();
				}
			}
		});

		SCHEMA = getScheme();
		VALUE_PATHS = getValuePaths();
		for (String valuePath : VALUE_PATHS) {
			expressionIdsPerValuePath.put(valuePath, new ArrayList<String>());
			values.put(valuePath, Collections
					.synchronizedList(new ArrayList<TimestampedValue>()));
		}

		initDefaultConfiguration(DEFAULT_CONFIGURATION);

	}

	public abstract void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION);

	public abstract String[] getValuePaths();

	/**
	 * Callback when connection to ContextDroid has been set up.
	 */
	protected void onConnected() {
		// no actions required
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onDestroy()
	 *
	 * Stops the connection to ContextDroid
	 */
	@Override
	public void onDestroy() {
		System.out.println("unbind context service from: " + getClass());
		contextServiceConnector.stop();
		super.onDestroy();
	}

	protected final void notifyDataChangedForId(String id) {
		String rootId = getRootIdFor(id);
		synchronized (this) {
			try {
				if (!notified.get(rootId)) {
					notified.put(rootId, true);
				}
			} catch (NullPointerException e) {
				// if it's no longer in the notified map
				return;
			}
		}
		contextServiceConnector.notifyDataChanged(new String[] { rootId });
	}

	private String getRootIdFor(String id) {
		while (id.endsWith(".R") || id.endsWith(".L")) {
			id = id.substring(0, id.length() - 2);
		}
		return id;
	}

	protected final void notifyDataChanged(String valuePath) {
		List<String> notify = new ArrayList<String>();

		synchronized (this) {
			for (String id : expressionIdsPerValuePath.get(valuePath)) {
				id = getRootIdFor(id);
				if (!notified.get(id)) {
					notify.add(id);
					notified.put(id, true);
				}
			}
		}

		if (notify.size() > 0) {
			contextServiceConnector.notifyDataChanged(notify
					.toArray(new String[notify.size()]));
		}
	}

	protected abstract void register(String id, String valuePath,
			Bundle configuration);

	protected abstract void unregister(String id);

	protected String getScheme() {
		return SCHEMA;
	}

	private void printState() {
		for (String key : notified.keySet()) {
			System.out.println("not: " + key + ": " + notified.get(key));
		}
		for (String key : registeredConfigurations.keySet()) {
			System.out.println("conf: " + key + ": "
					+ registeredConfigurations.get(key));
		}
		for (String key : registeredValuePaths.keySet()) {
			System.out.println("vp: " + key + ": "
					+ registeredValuePaths.get(key));
		}
		for (String key : expressionIdsPerValuePath.keySet()) {
			System.out.println("expressionIds: " + key + ": "
					+ expressionIdsPerValuePath.get(key));
		}

	}

	/** The m binder. */
	private final IAsynchronousContextSensor.Stub mBinder = new IAsynchronousContextSensor.Stub() {

		@Override
		public void register(String id, String valuePath, Bundle configuration)
				throws RemoteException {
			// any calls to register should wait until we're connected back to
			// the context service

			while (!contextServiceConnector.isConnected()) {
				synchronized (contextServiceConnector) {
					try {
						contextServiceConnector.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			synchronized (AbstractAsynchronousSensor.this) {
				notified.put(getRootIdFor(id), false);
				registeredConfigurations.put(id, configuration);
				registeredValuePaths.put(id, valuePath);
				expressionIdsPerValuePath.get(valuePath).add(id);
				// printState();

				AbstractAsynchronousSensor.this.register(id, valuePath,
						configuration);
			}
		}

		@Override
		public void unregister(String id) throws RemoteException {
			notified.remove(getRootIdFor(id));
			registeredConfigurations.remove(id);
			String valuePath = registeredValuePaths.remove(id);
			expressionIdsPerValuePath.get(valuePath).remove(id);
			// printState();
			AbstractAsynchronousSensor.this.unregister(id);
		}

		@Override
		public List<TimestampedValue> getValues(String id, long now,
				long timespan) throws RemoteException {
			synchronized (AbstractAsynchronousSensor.this) {
				notified.put(getRootIdFor(id), false);
			}
			try {
				return AbstractAsynchronousSensor.this.getValues(id, now,
						timespan);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		@Override
		public String getScheme() throws RemoteException {
			return AbstractAsynchronousSensor.this.getScheme();
		}

	};

	/**
	 * Gets all readings from timespan seconds ago until now. Readings are in
	 * reverse order (latest first). This is important for the expression
	 * engine.
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return All readings in the timespan between timespan seconds ago and now
	 */
	public static List<TimestampedValue> getValuesForTimeSpan(
			List<TimestampedValue> values, long now, long timespan) {
		// make a copy of the list
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();
		result.addAll(values);

		if (timespan == 0) {
			// only return latest
			return result
					.subList(Math.max(0, result.size() - 1), result.size());
		}

		int startPos = 0;
		int endPos = 0;
		for (int i = 0; i < values.size(); i++) {
			if ((now - timespan) > values.get(i).timestamp) {
				startPos++;
			}
			if (now > values.get(i).timestamp) {
				endPos++;
			}
		}
		return result.subList(startPos, endPos);

	}

	protected void trimValues(int history) {
		for (String path : VALUE_PATHS) {
			if (values.get(path).size() >= history) {
				values.get(path).remove(0);
			}
		}
	}


	protected void trimValueByTime(long expire) {
		for (String valuePath : VALUE_PATHS) {
			while ((values.get(valuePath).size() > 0
					&& values.get(valuePath).get(0).timestamp < expire)) {
				values.get(valuePath).remove(0);
			}
		}
	}

	protected void putValue(String valuePath, long now, long expire,
			Object value) {
		values.get(valuePath).add(
				new TimestampedValue(value, now, expire));
		notifyDataChanged(valuePath);
	}

	protected List<TimestampedValue> getValues(String id, long now, long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
