package interdroid.contextdroid.sensors;

import interdroid.vdb.content.avro.AvroContentProviderProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

/**
 * A sensor for location.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class LocationSensor extends AbstractAsynchronousSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(LocationSensor.class);

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
		String scheme =
				"{'type': 'record', 'name': 'location', "
						+ "'namespace': 'interdroid.context.sensor.location',"
						+ "\n'fields': ["
						+ SCHEMA_TIMESTAMP_FIELDS
						+ "\n{'name': '"
						+ LATITUDE_FIELD
						+ "', 'type': 'double'},"
						+ "\n{'name': '"
						+ LONGITUDE_FIELD
						+ "', 'type': 'double'},"
						+ "\n{'name': '"
						+ ALTITUDE_FIELD
						+ "', 'type': 'double'},"
						+ "\n{'name': '"
						+ SPEED_FIELD
						+ "', 'type': 'float'}"
						+ "\n]"
						+ "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * Default expiration time.
	 */
	public static final long EXPIRE_TIME = 1000;

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

			long expire = now + EXPIRE_TIME;
			ContentValues values = new ContentValues();
			LOG.debug("Location: {} {}", location.getLatitude(),
					location.getLongitude());
			values.put(LATITUDE_FIELD, location.getLatitude());
			values.put(LONGITUDE_FIELD, location.getLongitude());
			values.put(ALTITUDE_FIELD, location.getAltitude());
			values.put(SPEED_FIELD, location.getSpeed());

			putValues(values, now, expire);
		}

		public void onProviderDisabled(final String provider) {
			LOG.debug("provider disabled: {}. I am using: {}",
					provider, currentProvider);
			if (provider.equals(currentProvider)) {
				LOG.warn(
						"location sensor disabled due to lack of provider");
			}
		}

		public void onProviderEnabled(final String provider) {
			LOG.debug("provider enabled: {}", provider);
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			if (provider.equals(currentProvider)
					&& status != LocationProvider.AVAILABLE) {
				LOG.warn(
						"location sensor disabled because sensor unavailable");
			}

		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { LATITUDE_FIELD, LONGITUDE_FIELD, ALTITUDE_FIELD,
				SPEED_FIELD};
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putLong(MIN_TIME, 0);
		defaults.putLong(MIN_DISTANCE, 0);
		defaults.putString(PROVIDER,
				LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		locationManager =
				(LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	protected final void register(final String id, final String valuePath,
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
		String mostAccurateProvider = LocationManager.PASSIVE_PROVIDER;

		for (Bundle configuration : registeredConfigurations.values()) {
			if (configuration.containsKey(MIN_TIME)) {
				minTime = Math.min(minTime, configuration.getLong(MIN_TIME));
			}
			if (configuration.containsKey(MIN_DISTANCE)) {
				minDistance = Math.min(minDistance,
						configuration.getLong(MIN_DISTANCE));
			}
			if (configuration.containsKey(PROVIDER)) {
				if (LocationManager.PASSIVE_PROVIDER
						.equals(mostAccurateProvider)) {
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
			minTime = DEFAULT_CONFIGURATION.getLong(MIN_TIME);
		}
		if (minDistance == Long.MAX_VALUE) {
			minDistance = DEFAULT_CONFIGURATION.getLong(MIN_DISTANCE);
		}

		locationManager.removeUpdates(locationListener);
		locationManager.requestLocationUpdates(mostAccurateProvider, minTime,
				minDistance, locationListener, Looper.getMainLooper());
	}

	@Override
	protected final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			locationManager.removeUpdates(locationListener);
		} else {
			updateListener();
		}

	}

}
