package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.contextexpressions.ContextTypedValue;

import java.util.ArrayList;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

/**
 * Keeps track of context sensors
 */
public class SensorManager {

	/** Logging tag. */
	private static final String TAG = "SensorManager";

	/** The sensor information */
	private List<SensorServiceInfo> sensorList = new ArrayList<SensorServiceInfo>();

	/** The service connections */
	private List<ServiceConnection> connectionList = new ArrayList<ServiceConnection>();

	/** The context (for launching new services). */
	private Context context;

	/**
	 * Instantiates a new sensor manager.
	 * 
	 * @param context
	 *            the context to launch new services in
	 */
	public SensorManager(Context context) {
		this.context = context;
	}

	public void bindToSensor(ContextTypedValue value,
			ServiceConnection connection) throws SensorConfigurationException,
			SensorInitializationFailedException {
		try {
			bindToSensor(value, connection, false);
		} catch (SensorInitializationFailedException e) {
			// retry with discovery to true
			bindToSensor(value, connection, true);
		}
		// store the service connection so that we can use it to unbind later
		// on.
		connectionList.add(connection);
	}

	private void bindToSensor(ContextTypedValue value,
			ServiceConnection connection, boolean discover)
			throws SensorConfigurationException,
			SensorInitializationFailedException {
		if (discover) {
			discover();
		}
		for (SensorServiceInfo sensorInfo : sensorList) {
			if (sensorInfo.getEntity().equals(value.getEntity())
					&& sensorInfo.getValuePaths()
							.contains(value.getValuePath())
					&& sensorInfo
							.acceptsConfiguration(value.getConfiguration())) {
				// bind to the sensor
				context.bindService(sensorInfo.getIntent(), connection,
						Context.BIND_AUTO_CREATE);
				return;
			}
		}
		throw new SensorInitializationFailedException(
				"Failed to bind to service for: " + value.toString());
	}

	public void unbindSensor(ServiceConnection connection) {
		context.unbindService(connection);
	}

	private void discover() {
		sensorList.clear();
		sensorList = discover(context);
	}

	public static List<SensorServiceInfo> discover(Context context) {
		List<SensorServiceInfo> result = new ArrayList<SensorServiceInfo>();
		Log.d(TAG, "Starting sensor discovery");
		PackageManager pm = context.getPackageManager();
		Intent queryIntent = new Intent(
				"interdroid.contextdroid.sensor.DISCOVER");
		List<ResolveInfo> discoveredSensors = pm.queryIntentServices(
				queryIntent, PackageManager.GET_META_DATA);
		Log.d(TAG, "Found " + discoveredSensors.size() + " sensors");
		for (ResolveInfo discoveredSensor : discoveredSensors) {
			Log.d(TAG, "\tDiscovered sensor: "
					+ discoveredSensor.serviceInfo.packageName
					+ discoveredSensor.serviceInfo.name);
			result.add(new SensorServiceInfo(new ComponentName(
					discoveredSensor.serviceInfo.packageName,
					discoveredSensor.serviceInfo.name),
					discoveredSensor.serviceInfo.metaData));
		}
		return result;
	}

	public void unbindAllSensors() {
		for (ServiceConnection connection : connectionList) {
			context.unbindService(connection);
		}
	}

}
