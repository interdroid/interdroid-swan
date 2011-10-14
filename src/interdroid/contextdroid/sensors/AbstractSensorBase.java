package interdroid.contextdroid.sensors;

import interdroid.contextdroid.ConnectionListener;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

public abstract class AbstractSensorBase extends Service implements SensorInterface {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(AbstractSensorBase.class);

	/**
	 * Gets all readings from timespan seconds ago until now. Readings are in
	 * reverse order (latest first). This is important for the expression
	 * engine.
	 *
	 * @param now
	 *            the start
	 * @param timespan
	 *            the end
	 * @return All readings in the timespan between timespan seconds ago and now
	 */
	protected static final List<TimestampedValue> getValuesForTimeSpan(final List<TimestampedValue> values,
			long now, long timespan) {
				// make a copy of the list
				List<TimestampedValue> result;

				if (timespan == 0) {
					result =  values.
							subList(Math.max(0, values.size() - 1), values.size());
				} else {
					result = new ArrayList<TimestampedValue>();
					result.addAll(values);

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
					result = result.subList(startPos, endPos);
				}
				return result;
			}

	private final SensorInterface mSensorInterface = this;

	protected final String[] VALUE_PATHS = getValuePaths();

	protected final Bundle mDefaultConfiguration = new Bundle();

	protected final Bundle currentConfiguration = new Bundle();

	protected final Map<String, Bundle> registeredConfigurations =
			new HashMap<String, Bundle>();

	protected final Map<String, String> registeredValuePaths = new
			HashMap<String, String>();

	protected final Map<String, List<String>> expressionIdsPerValuePath =
			new HashMap<String, List<String>>();

	/** The context manager. */
	protected SensorContextServiceConnector contextServiceConnector;

	private HashMap<String, Boolean> notified = new HashMap<String, Boolean>();

	public abstract void initDefaultConfiguration(Bundle defaults);

	public abstract String[] getValuePaths();

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onCreate()
	 *
	 * Creates the ContextManager and connects to the ContextDroid service.
	 */
	@Override
	public final void onCreate() {
		contextServiceConnector = new SensorContextServiceConnector(this);
		contextServiceConnector.start(new ConnectionListener() {

			@Override
			public void onDisconnected() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onConnected() {
				AbstractSensorBase.this.onConnected();
				synchronized (contextServiceConnector) {
					contextServiceConnector.notifyAll();
				}
			}
		});

		init();
		initDefaultConfiguration(mDefaultConfiguration);
	}

	protected abstract void init();

	/** The binder. */
	private final IAsynchronousContextSensor.Stub mBinder =
			new IAsynchronousContextSensor.Stub() {

		@Override
		public void register(final String id, final String valuePath,
				final Bundle configuration)
				throws RemoteException {

			// TODO: We should be checking if valuePath exists and if id is
			// unique.

			// any calls to register should wait until we're connected back to
			// the context service
			synchronized (contextServiceConnector) {
				while (!contextServiceConnector.isConnected()) {
					LOG.debug("Waiting for registration to complete.");
					try {
						contextServiceConnector.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}

			synchronized (mSensorInterface) {
				try {
					LOG.debug("Registering id: {} path: {}", id, valuePath);
					notified.put(getRootIdFor(id), false);
					registeredConfigurations.put(id, configuration);
					registeredValuePaths.put(id, valuePath);
					List<String> ids = expressionIdsPerValuePath.get(valuePath);
					if (ids == null) {
						ids = new ArrayList<String>();
						expressionIdsPerValuePath.put(valuePath, ids);
					}
					ids.add(id);
					if (LOG.isDebugEnabled()) {
						printState();
					}
					LOG.debug("Registering with implementation.");
					mSensorInterface.register(id, valuePath, configuration);
				} catch (RuntimeException e) {
					LOG.error("Caught exception while registering.", e);
					throw e;
				}
			}
		}

		@Override
		public void unregister(final String id) throws RemoteException {
			notified.remove(getRootIdFor(id));
			registeredConfigurations.remove(id);
			String valuePath = registeredValuePaths.remove(id);
			expressionIdsPerValuePath.get(valuePath).remove(id);
			if (LOG.isDebugEnabled()) {
				printState();
			}

			mSensorInterface.unregister(id);
		}

		@Override
		public List<TimestampedValue> getValues(String id, long now,
				long timespan) throws RemoteException {
			synchronized (AbstractSensorBase.this) {
				notified.put(getRootIdFor(id), false);
			}
			try {
				return mSensorInterface.getValues(id, now,
						timespan);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		@Override
		public String getScheme() throws RemoteException {
			return mSensorInterface.getScheme();
		}

	};

	private void printState() {
		for (String key : notified.keySet()) {
			LOG.debug("not: {} : {}", key, notified.get(key));
		}
		for (String key : registeredConfigurations.keySet()) {
			LOG.debug("conf: {} : {}", key, registeredConfigurations.get(key));
		}
		for (String key : registeredValuePaths.keySet()) {
			LOG.debug("vp: {} : {}", key, registeredValuePaths.get(key));
		}
		for (String key : expressionIdsPerValuePath.keySet()) {
			LOG.debug("expressionIds: {} : {}", key,
					expressionIdsPerValuePath.get(key));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onBind(android.content.Intent)
	 *
	 * returns the sensor interface
	 */
	@Override
	public final IBinder onBind(Intent arg0) {
		return mBinder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onDestroy()
	 *
	 * Stops the connection to ContextDroid
	 */
	@Override
	public final void onDestroy() {
		LOG.debug("unbind context service from: {}", getClass());
		contextServiceConnector.stop();
		mSensorInterface.onDestroySensor();
		super.onDestroy();
	}

	// =-=-=-=- Utility Functions -=-=-=-=

	/**
	 * @param id the id to find the root of
	 * @return the id of the root expression
	 */
	protected String getRootIdFor(String id) {
		while (id.endsWith(".R") || id.endsWith(".L")) {
			id = id.substring(0, id.length() - 2);
		}
		return id;
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
}
