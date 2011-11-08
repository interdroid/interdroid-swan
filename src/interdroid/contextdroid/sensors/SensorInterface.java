package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.List;

import android.os.Bundle;

/**
 * This is the interface that sensors which make use of the
 * AbstractSensorBase or one of its subclasses must implement.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public interface SensorInterface {

	/**
	 * Handle registering an expression.
	 * @param id the expression to register
	 * @param valuePath the value path being registered
	 * @param configuration the configuration for the expression
	 */
	void register(String id, String valuePath,
			Bundle configuration);

	/**
	 * Handle unregistering an expression.
	 * @param id the expression to unregister
	 */
	void unregister(String id);

	/**
	 * @return the scheme for this sensor
	 */
	String getScheme();

	/**
	 *
	 * @param id the id of the expression to use
	 * @param now the time right now
	 * @param timespan the timespan desired
	 * @return the values requested
	 */
	List<TimestampedValue> getValues(String id, long now,
			long timespan);

	/**
	 * @return the value paths this sensor puts out
	 */
	String[] getValuePaths();

	/**
	 * Callback when a sensor is being destroyed.
	 */
	void onDestroySensor();

	/**
	 * Callback when connection to ContextDroid has been set up.
	 */
	void onConnected();

}
