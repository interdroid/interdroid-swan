package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.SensorServiceInfo;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.ServiceConnection;

/**
 * Keeps track of context sensors.
 */
public class SensorManager {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(SensorManager.class);


	/** The sensor information. */
	private List<SensorServiceInfo> sensorList =
			new ArrayList<SensorServiceInfo>();

	/** The service connections. */
	private final List<ServiceConnection> connectionList =
			new ArrayList<ServiceConnection>();

	/** The context (for launching new services). */
	private final Context context;

	/**
	 * Instantiates a new sensor manager.
	 *
	 * @param appContext
	 *            the context to launch new services in
	 */
	public SensorManager(final Context appContext) {
		this.context = appContext;
	}

	/**
	 * Binds to a given sensor.
	 * @param value the value to bind for
	 * @param connection the connection to use
	 * @throws SensorConfigurationException if the sensor is miss-configured
	 * @throws SensorSetupFailedException if setup fails
	 */
	public final void bindToSensor(final ContextTypedValue value,
			final ServiceConnection connection)
					throws SensorConfigurationException,
			SensorSetupFailedException {
		try {
			bindToSensor(value, connection, false);
		} catch (SensorSetupFailedException e) {
			// retry with discovery to true
			bindToSensor(value, connection, true);
		}
		// store the service connection so that we can use it to unbind later
		// on.
		connectionList.add(connection);
	}

	/**
	 * Binds to a given sensor.
	 * @param value The value to bind for
	 * @param connection The connection to use
	 * @param discover true if we should run discover() first
	 * @throws SensorConfigurationException if the sensor is misconfigured
	 * @throws SensorSetupFailedException if setup fails.
	 */
	private void bindToSensor(final ContextTypedValue value,
			final ServiceConnection connection, final boolean discover)
			throws SensorConfigurationException,
			SensorSetupFailedException {
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
		throw new SensorSetupFailedException(
				"Failed to bind to service for: " + value.toString());
	}

	/**
	 * Unbinds a sensor.
	 * @param connection the connection to unbind with.
	 */
	public final void unbindSensor(final ServiceConnection connection) {
		context.unbindService(connection);
	}

	/**
	 * Runs sensor discovery.
	 */
	private void discover() {
		sensorList.clear();
		LOG.debug("Starting sensor discovery");
		sensorList = ContextManager.getSensors(context);
	}

	/**
	 * Unbinds all bound sensors.
	 */
	public final void unbindAllSensors() {
		for (ServiceConnection connection : connectionList) {
			context.unbindService(connection);
		}
	}

}
