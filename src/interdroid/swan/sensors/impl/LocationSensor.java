package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import java.lang.reflect.Field;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

/**
 * A sensor for location.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class LocationSensor extends AbstractVdbSensor {

	private static final String TAG = "Location Sensor";

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
			return R.xml.location_preferences;
		}

	}

	/**
	 * Latitude field name.
	 */
	public static final String LATITUDE_FIELD = "latitude";
	/**
	 * Longitude field name.
	 */
	public static final String LONGITUDE_FIELD = "longitude";
	/**
	 * Altitude field name.
	 */
	public static final String ALTITUDE_FIELD = "altitude";
	/**
	 * Speed field name.
	 */
	public static final String SPEED_FIELD = "speed";

	/**
	 * Minimum acceptable distance.
	 */
	public static final String MIN_DISTANCE = "min_distance";
	/**
	 * Minimum acceptable time.
	 */
	public static final String MIN_TIME = "min_time";
	/**
	 * The type of provider desired.
	 */
	public static final String PROVIDER = "provider";

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
		String scheme = "{'type': 'record', 'name': 'location', "
				+ "'namespace': 'interdroid.context.sensor.location',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + "\n{'name': '"
				+ LATITUDE_FIELD + "', 'type': 'double'}," + "\n{'name': '"
				+ LONGITUDE_FIELD + "', 'type': 'double'}," + "\n{'name': '"
				+ ALTITUDE_FIELD + "', 'type': 'double'}," + "\n{'name': '"
				+ SPEED_FIELD + "', 'type': 'float'}" + "\n]" + "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The current provider we are using.
	 */
	private String currentProvider;

	/**
	 * The location manager we use.
	 */
	private LocationManager locationManager;

	/**
	 * The location listener.
	 */
	private LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(final Location location) {
			long now = System.currentTimeMillis();

			ContentValues values = new ContentValues();
			Log.d(TAG,
					"Location: " + location.getLatitude() + ", "
							+ location.getLongitude());
			values.put(LATITUDE_FIELD, location.getLatitude());
			values.put(LONGITUDE_FIELD, location.getLongitude());
			values.put(ALTITUDE_FIELD, location.getAltitude());
			values.put(SPEED_FIELD, location.getSpeed());

			putValues(values, now);
		}

		public void onProviderDisabled(final String provider) {
			Log.d(TAG, "provider disabled: " + provider + ". I am using: "
					+ currentProvider);
			if (provider.equals(currentProvider)) {
				Log.w(TAG, "location sensor disabled due to lack of provider");
			}
		}

		public void onProviderEnabled(final String provider) {
			Log.d(TAG, "provider enabled: " + provider);
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			if (provider.equals(currentProvider)
					&& status != LocationProvider.AVAILABLE) {
				Log.w(TAG,
						"location sensor disabled because sensor unavailable");
			}

		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { LATITUDE_FIELD, LONGITUDE_FIELD, ALTITUDE_FIELD,
				SPEED_FIELD };
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putLong(MIN_TIME, 0);
		defaults.putLong(MIN_DISTANCE, 0);
		defaults.putString(PROVIDER, LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() > 0) {
			updateListener();
		}
	}

	/**
	 * Updates the listener.
	 */
	private void updateListener() {
		long minTime = Long.MAX_VALUE;
		long minDistance = Long.MAX_VALUE;
		String mostAccurateProvider;
		String passiveProvider = null;
		// Reflect out PASSIVE_PROVIDER so we can still run on 7.
		try {
			Field passive = LocationManager.class.getField("PASSIVE_PROVIDER");
			passiveProvider = (String) passive.get(null);
			mostAccurateProvider = passiveProvider;
		} catch (Exception e) {
			Log.w(TAG, "Caught exception checking for PASSIVE_PROVIDER.", e);
			mostAccurateProvider = LocationManager.NETWORK_PROVIDER;
		}

		for (Bundle configuration : registeredConfigurations.values()) {
			if (configuration.containsKey(MIN_TIME)) {
				minTime = Math.min(minTime, configuration.getLong(MIN_TIME));
			}
			if (configuration.containsKey(MIN_DISTANCE)) {
				minDistance = Math.min(minDistance,
						configuration.getLong(MIN_DISTANCE));
			}
			if (configuration.containsKey(PROVIDER)) {
				if (mostAccurateProvider.equals(passiveProvider)) {
					// if current is passive, anything is better
					mostAccurateProvider = configuration.getString(PROVIDER);
				} else if (LocationManager.NETWORK_PROVIDER
						.equals(mostAccurateProvider)
						&& LocationManager.GPS_PROVIDER.equals(configuration
								.getString(PROVIDER))) {
					// if current is network, only gps is better
					mostAccurateProvider = LocationManager.GPS_PROVIDER;
				}
				// if it isn't PASSIVE or NETWORK, we can't do any better, it
				// must be GPS
			}
		}
		if (minTime == Long.MAX_VALUE) {
			minTime = mDefaultConfiguration.getLong(MIN_TIME);
		}
		if (minDistance == Long.MAX_VALUE) {
			minDistance = mDefaultConfiguration.getLong(MIN_DISTANCE);
		}

		locationManager.removeUpdates(locationListener);
		locationManager.requestLocationUpdates(mostAccurateProvider, minTime,
				minDistance, locationListener, Looper.getMainLooper());
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			locationManager.removeUpdates(locationListener);
		} else {
			updateListener();
		}

	}

	@Override
	public void onDestroySensor() {
		// TODO Auto-generated method stub

	}

}
