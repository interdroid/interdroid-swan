package interdroid.swan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

/**
 * Stores and keeps tracks of sensor service information.
 */
public class SensorInfo {

	private static final String TAG = "Sensor Info";

	/**
	 * The name of the component the sensor runs in..
	 */
	private final ComponentName component;

	/**
	 * The entity Id for this service.
	 */
	private final String entityId;

	/**
	 * The authority for this sensors database.
	 */
	private final String authority;

	/**
	 * The value paths this sensor supports.
	 */
	private final ArrayList<String> valuePaths = new ArrayList<String>();

	/**
	 * The units the valuepath returns.
	 */
	private final ArrayList<String> units = new ArrayList<String>();

	/**
	 * The Drawable icon for this sensor.
	 */

	private Drawable icon;

	/**
	 * The configuration for this sensor.
	 */
	private final Bundle configuration;

	/**
	 * Construct information about a sensor.
	 * 
	 * @param sensorComponent
	 *            The component the sensor lives in
	 * @param metaData
	 *            metadata bundle for the service from the package manager
	 */
	public SensorInfo(final ComponentName sensorComponent,
			final Bundle metaData, Drawable icon) {
		this.component = sensorComponent;
		this.icon = icon;
		// strip out the entityId
		if (metaData == null) {
			throw new IllegalArgumentException("no metadata!");
		}
		entityId = metaData.getString("entityId");
		metaData.remove("entityId");
		authority = metaData.getString("authority");
		metaData.remove("authority");

		// and the units
		units.addAll(Arrays.asList(metaData.getString("units").split(",", -1)));
		metaData.remove("units");

		// and the value paths
		valuePaths.addAll(Arrays.asList(metaData.getString("valuePaths").split(
				",")));
		metaData.remove("valuePaths");

		// all other items in the bundle are supposed to be configurable (values
		// are default values)
		configuration = metaData;

		Set<String> keys = new HashSet<String>();
		keys.addAll(metaData.keySet());
		for (String key : keys) {
			if (metaData.get(key).toString().endsWith("L")) {
				try {
					long longValue = Long
							.parseLong(metaData.getString(key).substring(0,
									metaData.getString(key).length() - 1));
					metaData.remove(key);
					metaData.putLong(key, longValue);
				} catch (NumberFormatException e) {
					Log.d(TAG, "Can't convert number. Using string.");
				}
			}
			if (metaData.get(key).toString().endsWith("D")) {
				try {
					double doubleValue = Double.parseDouble(metaData.getString(
							key).substring(0,
							metaData.getString(key).length() - 1));
					metaData.remove(key);
					metaData.putDouble(key, doubleValue);
				} catch (NumberFormatException e) {
					Log.d(TAG, "Can't convert number. Using string.");
				}
			}
		}

	}

	/**
	 * 
	 * @return icon of the sensor
	 */
	public Drawable getIcon() {
		return icon;
	}

	/**
	 * @return the entity Id for this sensor.
	 */
	public final String getEntity() {
		return entityId;
	}

	/**
	 * @return the value paths this sensor supports.
	 */
	public final ArrayList<String> getValuePaths() {
		return valuePaths;
	}

	/**
	 * @return the units this sensor supports.
	 */
	public final ArrayList<String> getUnits() {
		return units;
	}

	/**
	 * @return the authority for this sensors database if appropriate.
	 */
	public final String getAuthority() {
		return authority;
	}

	/**
	 * Checks if this sensor accepts the given configuration.
	 * 
	 * @param b
	 *            the bundle to check
	 * @return true if this sensor supports the given bundle
	 * @throws SensorConfigurationException
	 *             if the sensor is not configured
	 */
	public final boolean acceptsConfiguration(final Bundle b)
			throws SensorConfigurationException {
		if (b != null) {
			Set<String> keys = new HashSet<String>();
			keys.addAll(b.keySet());
			for (String key : keys) {
				if (configuration.containsKey(key)) {
					if (!b.get(key).getClass()
							.isInstance(configuration.get(key))) {
						try {
							String value = b.getString(key);
							b.remove(key);
							if (configuration.get(key).getClass() == Integer.class) {
								b.putInt(key, Integer.parseInt(value));
							} else if (configuration.get(key).getClass() == Long.class) {
								b.putLong(key, Long.parseLong(value));
							} else if (configuration.get(key).getClass() == Float.class) {
								b.putFloat(key, Float.parseFloat(value));
							} else if (configuration.get(key).getClass() == Double.class) {
								b.putDouble(key, Double.parseDouble(value));
							} else if (configuration.get(key).getClass() == Boolean.class) {
								b.putBoolean(key, Boolean.parseBoolean(value));
							}
						} catch (NumberFormatException e) {
							throw new SensorConfigurationException(
									"Unable to parse value for configuration '"
											+ key + "' in entity " + entityId
											+ " to type "
											+ configuration.get(key).getClass());
						}
					}
				} else {
					throw new SensorConfigurationException(
							"Unsupported configuration key '" + key + "' for "
									+ entityId);
				}
			}
			for (String key : configuration.keySet()) {
				if ("required".equals(configuration.get(key))) {
					if (b.containsKey(key)) {
						continue;
					} else {
						throw new SensorConfigurationException(
								"Missing required configuration key '" + key
										+ "' for " + entityId + " value: "
										+ configuration.get(key));
					}
				}
			}
		}
		return true;
	}

	/**
	 * Returns an intent to launch this sensor.
	 * 
	 * @return new Intent().setComponent(getComponent())
	 */
	public final Intent getIntent() {
		ComponentName serviceComponent = new ComponentName(
				component.getPackageName(), component.getClassName().replace(
						"$ConfigurationActivity", ""));
		return new Intent().setComponent(serviceComponent);
	}

	/**
	 * Returns an intent with component.getClassName() +
	 * "$ConfigurationActivity" for launching the sensors configuration
	 * activity.
	 * 
	 * @return an intent for launching the configuration activity
	 */
	public final Intent getConfigurationIntent() {
		Intent result = new Intent().setComponent(component);
		result.putExtra("entityId", entityId);
		return result;
	}

	@Override
	public String toString() {
		return entityId;
	}
}
