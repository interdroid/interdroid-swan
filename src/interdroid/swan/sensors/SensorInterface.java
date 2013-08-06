package interdroid.swan.sensors;

import interdroid.swan.swansong.TimestampedValue;

import java.io.IOException;
import java.util.List;

import android.os.Bundle;

/**
 * This is the interface that sensors which make use of the AbstractSensorBase
 * or one of its subclasses must implement.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public interface SensorInterface {

	public static final String ACTION_NOTIFY = "interdroid.swan.NOTIFY";

	/**
	 * Handle registering an expression.
	 * 
	 * @param id
	 *            the expression to register
	 * @param valuePath
	 *            the value path being registered
	 * @param configuration
	 *            the configuration for the expression
	 * @throws IOException
	 *             if there is a problem with the sensor
	 */
	void register(String id, String valuePath, Bundle configuration)
			throws IOException;

	/**
	 * Handle unregistering an expression.
	 * 
	 * @param id
	 *            the expression to unregister
	 */
	void unregister(String id);

	/**
	 * 
	 * @param id
	 *            the id of the expression to use
	 * @param now
	 *            the time right now
	 * @param timespan
	 *            the timespan desired
	 * @return the values requested
	 */
	List<TimestampedValue> getValues(String id, long now, long timespan);

	/**
	 * @return the value paths this sensor puts out
	 */
	String[] getValuePaths();

	/**
	 * Callback when a sensor is being destroyed.
	 */
	void onDestroySensor();

	/**
	 * Callback when connection to Swan has been set up.
	 */
	void onConnected();

	/**
	 * How long it takes until this sensor is up and running for the given id
	 * 
	 * @param id
	 * @return
	 */
	long getStartUpTime(String id);
	
	
	double getAverageSensingRate();
}
