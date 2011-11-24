package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
public class SmartLocationSensor extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(SmartLocationSensor.class);

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
	 * Vicinity field name.
	 */
	public static final String VICINITY = "vicinity";

	/**
	 * Within field name
	 */
	public static final String WITHIN = "within";

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
				+ "'namespace': 'interdroid.context.sensor.smartlocation',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + "\n{'name': '"
				+ VICINITY + "', 'type': 'integer'}" + "\n]" + "}";
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

			// update for vicinity
			float[] results = new float[3];
			float distance;
			long minDelay = Long.MAX_VALUE;
			for (int i = 0; i < mVicinityLatitudes.size(); i++) {
				ContentValues values = new ContentValues();
				Location.distanceBetween(location.getLatitude(),
						location.getLongitude(), mVicinityLatitudes.get(i),
						mVicinityLongitudes.get(i), results);
				distance = results[0];
				int closeTo = (int) Math.log10(distance);

				minDelay = Math
						.min(minDelay,
								(long) (Math.min(
										distance - Math.pow(10, closeTo),
										Math.pow(10, closeTo + 1) - distance) / mVicinityMaxSpeeds
										.get(i)));
				values.put(VICINITY, closeTo);
				putValues(values, now);
			}
			// update for within
			for (int i = 0; i < mWithinLatitudes.size(); i++) {
				ContentValues values = new ContentValues();
				Location.distanceBetween(location.getLatitude(),
						location.getLongitude(), mWithinLatitudes.get(i),
						mWithinLongitudes.get(i), results);
				distance = results[0];
				minDelay = Math
						.min(minDelay, (long) (Math.abs(distance
								- mWithinThresholds.get(i)) / mWithinMaxSpeeds
								.get(i)));
				values.put(WITHIN, distance);
				putValues(values, now);
			}

			// now sleep for minDelay

		}

		public void onProviderDisabled(final String provider) {
			LOG.debug("provider disabled: {}. I am using: {}", provider,
					currentProvider);
			if (provider.equals(currentProvider)) {
				LOG.warn("location sensor disabled due to lack of provider");
			}
		}

		public void onProviderEnabled(final String provider) {
			LOG.debug("provider enabled: {}", provider);
		}

		public void onStatusChanged(final String provider, final int status,
				final Bundle extras) {
			if (provider.equals(currentProvider)
					&& status != LocationProvider.AVAILABLE) {
				LOG.warn("location sensor disabled because sensor unavailable");
			}

		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { VICINITY, WITHIN, };
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

	private List<Double> mVicinityMaxSpeeds = new ArrayList<Double>();
	private List<Double> mVicinityLatitudes = new ArrayList<Double>();
	private List<Double> mVicinityLongitudes = new ArrayList<Double>();

	private List<Double> mWithinMaxSpeeds = new ArrayList<Double>();
	private List<Double> mWithinLatitudes = new ArrayList<Double>();
	private List<Double> mWithinLongitudes = new ArrayList<Double>();
	private List<Double> mWithinThresholds = new ArrayList<Double>();

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (valuePath.equals(WITHIN)) {
			mWithinLatitudes.add(configuration.getDouble(LATITUDE));
			mWithinLongitudes.add(configuration.getDouble(LONGITUDE));
			mWithinMaxSpeeds.add(configuration.getDouble(MAX_SPEED));
			mWithinThresholds.add(configuration.getDouble(THRESHOLD));
		} else if (valuePath.equals(VICINITY)) {
			mVicinityLatitudes.add(configuration.getDouble(LATITUDE));
			mVicinityLongitudes.add(configuration.getDouble(LONGITUDE));
			mVicinityMaxSpeeds.add(configuration.getDouble(MAX_SPEED));
		} else {
			throw new RuntimeException("invalid valuePath: '" + valuePath
					+ "' for SmartLocationSensor");
		}
		updateListener();
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
			LOG.warn("Caught exception checking for PASSIVE_PROVIDER.");
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
