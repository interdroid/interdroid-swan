package interdroid.swan.sensors;

import interdroid.swan.swansong.TimestampedValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class is the abstract base for all Sensor services. Sensor implementors
 * are advised to use AbstractVdbSensor or AbstractMemorySensor as a basis for
 * their sensors instead of using this class directly.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public abstract class AbstractSensorBase extends Service implements
		SensorInterface {

	private static final String TAG = "AbstractSensorBase";

	/**
	 * The sensor interface.
	 */
	private final SensorInterface mSensorInterface = this;

	private long mStartTime;

	// Designed for direct use by subclasses.
	/**
	 * The value paths we support.
	 */
	protected final String[] VALUE_PATHS = getValuePaths();

	/**
	 * The default configuration.
	 */
	protected final Bundle mDefaultConfiguration = new Bundle();

	/**
	 * The current configuration of the sensor.
	 */
	protected final Bundle currentConfiguration = new Bundle();

	/**
	 * The registered configurations for the sensor.
	 */
	protected final Map<String, Bundle> registeredConfigurations = new HashMap<String, Bundle>();

	/**
	 * The value paths registered as watched.
	 */
	protected final Map<String, String> registeredValuePaths = new HashMap<String, String>();

	/**
	 * The expression ids for each value path.
	 */
	protected final Map<String, List<String>> expressionIdsPerValuePath = new HashMap<String, List<String>>();

	/**
	 * Initializes the default configuration for this sensor.
	 * 
	 * @param defaults
	 *            the bundle to add defaults to
	 */
	public abstract void initDefaultConfiguration(Bundle defaults);

	/**
	 * Called when the sensor is starting to allow subclasses to handle any
	 * setup that needs to be done.
	 */
	protected abstract void init();

	@Override
	public abstract String[] getValuePaths();

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 * 
	 * Creates the ContextManager and connects to the Swan service.
	 */
	@Override
	public final void onCreate() {
		Log.d(TAG, "abstract sensor oncreate");
		mStartTime = System.currentTimeMillis();
		init();
		initDefaultConfiguration(mDefaultConfiguration);
		onConnected();
	}

	/** The binder. */
	private final Sensor.Stub mBinder = new Sensor.Stub() {

		@Override
		public void register(final String id, final String valuePath,
				final Bundle configuration) throws RemoteException {
			// value path exists and id is unique (enforced by evaluation
			// engine)
			synchronized (mSensorInterface) {
				try {
					Log.d(TAG, "Registering id: " + id + " value path: "
							+ valuePath);
					registeredConfigurations.put(id, configuration);
					registeredValuePaths.put(id, valuePath);
					List<String> ids = expressionIdsPerValuePath.get(valuePath);
					if (ids == null) {
						ids = new ArrayList<String>();
						expressionIdsPerValuePath.put(valuePath, ids);
					}
					ids.add(id);
					printState();
					Log.d(TAG, "Registering with implementation.");
					mSensorInterface.register(id, valuePath, configuration);
				} catch (Exception e) {
					Log.e(TAG, "Caught exception while registering.", e);
					throw new RemoteException();
				}
			}
		}

		@Override
		public void unregister(final String id) throws RemoteException {
			registeredConfigurations.remove(id);
			String valuePath = registeredValuePaths.remove(id);
			expressionIdsPerValuePath.get(valuePath).remove(id);
			printState();
			mSensorInterface.unregister(id);
		}

		@Override
		public List<TimestampedValue> getValues(final String id,
				final long now, final long timespan) throws RemoteException {
			try {
				return mSensorInterface.getValues(id, now, timespan);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		@Override
		public long getStartUpTime(String id) throws RemoteException {
			return mSensorInterface.getStartUpTime(id);
		}

		@Override
		public Bundle getInfo() throws RemoteException {
			Bundle info = new Bundle();
			info.putString("name", getClass().getName());
			int num = 0;
			for (Map.Entry<String, List<String>> entry : expressionIdsPerValuePath
					.entrySet()) {
				num += entry.getValue().size();
			}
			info.putInt("registeredids", num);
			info.putDouble("sensingRate", getAverageSensingRate());
			info.putLong("starttime", getStartTime());
			info.putFloat("currentMilliAmpere", getCurrentMilliAmpere());
			return info;
		}
	};

	/**
	 * Debug helper which prints the state for this sensor.
	 */
	private void printState() {
		for (String key : registeredConfigurations.keySet()) {
			Log.d(TAG,
					"configs: " + key + ": "
							+ registeredConfigurations.get(key));
		}
		for (String key : registeredValuePaths.keySet()) {
			Log.d(TAG,
					"valuepaths: " + key + ": " + registeredValuePaths.get(key));
		}
		for (String key : expressionIdsPerValuePath.keySet()) {
			Log.d(TAG, "expressionIds: " + key + ": "
					+ expressionIdsPerValuePath.get(key));
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
	public final IBinder onBind(final Intent arg0) {
		return mBinder;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 * 
	 * Stops the connection to Swan
	 */
	@Override
	public final void onDestroy() {
		try {
			mSensorInterface.onDestroySensor();
		} catch (Exception e) {
			Log.e(TAG, "Got exception destroying sensor service", e);
		}
		super.onDestroy();
	}

	// =-=-=-=- Utility Functions -=-=-=-=

	/**
	 * Send a notification that data changed for the given id.
	 * 
	 * @param id
	 *            the id of the value to notify for.
	 */
	protected final void notifyDataChangedForId(final String... ids) {
		Intent notifyIntent = new Intent(ACTION_NOTIFY);
		notifyIntent.putExtra("expressionIds", ids);
		System.out.println("sending data changed for: " + Arrays.toString(ids));
		sendBroadcast(notifyIntent);
	}

	@Override
	public long getStartUpTime(String id) {
		return 0;
	}

	/**
	 * Send a notification that data for the given value path changed.
	 * 
	 * @param valuePath
	 *            the value path to notify for.
	 */
	protected final void notifyDataChanged(final String valuePath) {
		List<String> notify = new ArrayList<String>();

		synchronized (mSensorInterface) {
			// can be null if multiple valuepaths are updated together and not
			// for all of them, there's an id registered.
			if (expressionIdsPerValuePath.get(valuePath) != null) {
				for (String id : expressionIdsPerValuePath.get(valuePath)) {
					notify.add(id);
				}
			}
		}

		if (notify.size() > 0) {
			notifyDataChangedForId(notify.toArray(new String[notify.size()]));
		}
	}

	/**
	 * Gets all readings from timespan seconds ago until now. Readings are in
	 * reverse order (latest first). This is important for the expression
	 * engine.
	 * 
	 * @param now
	 *            the start
	 * @param timespan
	 *            the end
	 * @param values
	 *            the values
	 * @return All readings in the timespan between timespan seconds ago and now
	 */
	protected static final List<TimestampedValue> getValuesForTimeSpan(
			final List<TimestampedValue> values, final long now,
			final long timespan) {
		// make a copy of the list
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();

		if (timespan == 0) {
			if (values != null && values.size() > 0) {
				result.add(values.get(0));
			}
		} else {
			int startPos = 0;
			if (values != null) {
				result.addAll(values);
				for (int i = 0; i < result.size(); i++) {
					if ((now - timespan) < result.get(i).getTimestamp()) {
						startPos++;
					}
				}
			}

			result = result.subList(0, startPos);
		}
		return result;
	}

	@Override
	public double getAverageSensingRate() {
		return (double) getReadings()
				/ ((System.currentTimeMillis() - mStartTime) / 1000.0);
	}

	public long getStartTime() {
		return mStartTime;
	}

	public abstract long getReadings();

	public float getCurrentMilliAmpere() {
		return -1;
	}
}
