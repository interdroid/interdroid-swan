package interdroid.contextdroid.contextservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Stores and keeps tracks of sensor service state
 */
public class SensorServiceInfo {

	private ComponentName component;

	private String entityId;

	private ArrayList<String> valuePaths = new ArrayList<String>();

	private Bundle configuration;

	public SensorServiceInfo(ComponentName component, Bundle metaData) {
		this.component = component;
		// strip out the entityId
		if (metaData == null) {
			throw new RuntimeException("no metadata!");
		}
		entityId = metaData.getString("entityId");
		metaData.remove("entityId");
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
					// ignore this, just keep the string version
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
					// ignore this, just keep the string version
				}
			}
		}

	}

	public String getEntity() {
		return entityId;
	}

	public ArrayList<String> getValuePaths() {
		return valuePaths;
	}

	public boolean acceptsConfiguration(Bundle b)
			throws SensorConfigurationException {
		if (b != null) {
			Set<String> keys = new HashSet<String>();
			keys.addAll(b.keySet());
			for (String key : keys) {
				if (configuration.containsKey(key)) {
					// We cannot do this, since parsing the configuration in
					// ContextTypedValue will always put a String value
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
									"Unable to parse value for configuration key '"
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
				if ("null".equals(configuration.get(key))) {
					if (b.containsKey(key)) {
						continue;
					} else {
						throw new SensorConfigurationException(
								"Missing required configuration key '" + key
										+ "' for " + entityId);
					}
				}
			}
		}
		return true;
	}

	public Intent getIntent() {
		return new Intent().setComponent(component);
	}

	public Intent getConfigurationIntent() {
		ComponentName configurationComponent = new ComponentName(
				component.getPackageName(), component.getClassName().replace(
						"Sensor", "ConfigurationActivity"));
		return new Intent().setComponent(configurationComponent);
	}
}
