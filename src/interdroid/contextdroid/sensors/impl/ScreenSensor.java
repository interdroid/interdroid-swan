package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractMemorySensor;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

/**
 * A sensor for if the screen is on or off.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ScreenSensor extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(ScreenSensor.class);

	/**
	 * Is screen on field.
	 */
	public static final String IS_SCREEN_ON_FIELD = "is_screen_on";

	/**
	 * Default expire time.
	 */
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?

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
		String scheme =
				"{'type': 'record', 'name': 'screen', "
						+ "'namespace': 'interdroid.context.sensor.screen',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ IS_SCREEN_ON_FIELD
						+ "', 'type': 'boolean'}"
						+ "\n]"
						+ "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The receiver of screen information.
	 */
	private BroadcastReceiver screenReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			long now = System.currentTimeMillis();
			long expire = now + EXPIRE_TIME;
			ContentValues values = new ContentValues();

			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				values.put(IS_SCREEN_ON_FIELD, false);
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				values.put(IS_SCREEN_ON_FIELD, true);
			}
			putValues(values, now, expire);
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
		LOG.debug("screen sensor connected");
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
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
