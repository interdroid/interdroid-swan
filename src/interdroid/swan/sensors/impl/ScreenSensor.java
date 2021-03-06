package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;

/**
 * A sensor for if the screen is on or off.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class ScreenSensor extends AbstractVdbSensor {

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.screen_preferences;
		}

	}

	/**
	 * Is screen on field.
	 */
	public static final String IS_SCREEN_ON_FIELD = "is_screen_on";

	/**
	 * The schema for this sensor.
	 */
	public static final String SCHEME = getSchema();

	/**
	 * The provider for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class Provider extends AvroContentProviderProxy {

		/**
		 * Construct the provider for this sensor.
		 */
		public Provider() {
			super(SCHEME);
		}

	}

	/**
	 * @return the schema for this sensor.
	 */
	private static String getSchema() {
		String scheme = "{'type': 'record', 'name': 'screen', "
				+ "'namespace': 'interdroid.context.sensor.screen',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + "\n{'name': '"
				+ IS_SCREEN_ON_FIELD + "', 'type': 'string'}" + "\n]" + "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The receiver of screen information.
	 */
	private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			long now = System.currentTimeMillis();
			ContentValues values = new ContentValues();

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				values.put(IS_SCREEN_ON_FIELD, "false");
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				values.put(IS_SCREEN_ON_FIELD, "true");
			}
			putValues(values, now);
		}

	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { IS_SCREEN_ON_FIELD };
	}

	@Override
	public void initDefaultConfiguration(final Bundle defaults) {
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			ContentValues values = new ContentValues();
			values.put(IS_SCREEN_ON_FIELD, "" + pm.isScreenOn());
			putValues(values, System.currentTimeMillis());

			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			registerReceiver(screenReceiver, filter);
		}
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(screenReceiver);
		}
	}

	@Override
	public final void onDestroySensor() {
		unregisterReceiver(screenReceiver);
	}

}
