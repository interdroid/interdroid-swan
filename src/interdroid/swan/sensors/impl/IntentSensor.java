package interdroid.swan.sensors.impl;

import interdroid.swan.SwanException;
import interdroid.swan.ContextTypedValueListener;
import interdroid.swan.R;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractMemorySensor;

import android.os.Bundle;

public class IntentSensor extends AbstractMemorySensor {

	public static final String TAG = "Intent";

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setResult(
					RESULT_OK,
					getIntent().putExtra("configuration",
							IntentSensor.STARTED_FIELD));
			finish();
		}

		@Override
		public final int getPreferencesXML() {
			return R.xml.intent_preferences;
		}

	}

	public static final String STARTED_FIELD = "started";

	protected static final int HISTORY_SIZE = 10;

	private static final String MAGIC_RELAY = "MAGIC_RELAY";

	@Override
	public String[] getValuePaths() {
		return new String[] { STARTED_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'intent', " +
				"'namespace': 'context.sensor.intent',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ STARTED_FIELD
				+ "', 'type': 'string'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public void register(final String id, final String valuePath,
			final Bundle configuration) {
		try {
			contextServiceConnector.registerContextTypedValue(id + "."
					+ MAGIC_RELAY, new ContextTypedValue(
					"logcat" + ContextTypedValue.ENTITY_VALUE_PATH_SEPARATOR
					+ "log?logcat_parameters=ActivityManager:I *:S"),
					new ContextTypedValueListener() {

						@Override
						public void onReading(String relayedId,
								TimestampedValue[] newValues) {
							// values is always of length 1
							if (newValues[0].getValue().toString()
									.contains("Starting: Intent {")) {
								if (getValues().size() >= HISTORY_SIZE) {
									getValues().remove(0);
								}
								putValueTrimSize(valuePath, id,
										newValues[0].getTimestamp(),
										getIntentFrom(newValues[0].getValue()),
										HISTORY_SIZE);
							}

						}

					});
		} catch (SwanException e) {
			e.printStackTrace();
		}
	}

	private String getIntentFrom(final Object value) {
		String string = value.toString();
		string = string.substring(string.indexOf("cmp=") + 4);
		string = string.substring(0, string.indexOf(" "));
		return string;
	}

	@Override
	public void unregister(final String id) {
		try {
			contextServiceConnector.unregisterContextTypedValue(id + "."
					+ MAGIC_RELAY);
		} catch (SwanException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroySensor() {
		// Nothing to do
	}
}
