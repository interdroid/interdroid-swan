package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

/**
 * A sensor for location.
 * 
 * @author rkemp &lt;rkemp@cs.vu.nl&gt;
 * 
 */
public class SmartLocationSensor extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(SmartLocationSensor.class);

	private static final long SECOND = 1000; // ms

	// TODO make this configurable?
	private static final long MIN_TIME_BETWEEN_UPDATES = 20 * SECOND; // ms

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author rkemp &lt;rkemp@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.smart_location_preferences;
		}

	}

	/**
	 * Vicinity field name. Vicinity gives the 10 log of the distance in meters
	 * to the configured destination.
	 */
	public static final String VICINITY = "vicinity";

	/**
	 * Within field name. Within gives "true" when within the specified rang to
	 * the configured destination, "false" otherwise.
	 */
	public static final String WITHIN = "within";

	/**
	 * The type of provider desired.
	 */
	public static final String PROVIDER = "provider";

	/**
	 * The latitude.
	 */
	public static final String LATITUDE = "latitude";

	/**
	 * The longitude.
	 */
	public static final String LONGITUDE = "longitude";

	/**
	 * The max speed.
	 */
	public static final String MAX_SPEED = "max_speed";

	/**
	 * The within range.
	 */
	public static final String WITHIN_RANGE = "range";

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
		String scheme = "{'type': 'record', 'name': 'smartlocation', "
				+ "'namespace': 'interdroid.context.sensor.smartlocation',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + SCHEMA_ID_FIELDS
				+ "\n{'name': '" + VICINITY + "', 'type': 'int'}, {'name': '"
				+ WITHIN_RANGE + "', 'type': 'int'}" + "\n]" + "}";
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
	 * The requestSingleUpdate method (API 9+)
	 */
	private Method mRequestSingleUpdateMethod;

	/**
	 * The location listener.
	 */
	private LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(final Location location) {
			// if we couldn't use the requestSingleUpdate method, we have to
			// stop listening explicitly
			if (mRequestSingleUpdateMethod == null) {
				stopListener();
			}

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
				int vicinity = (int) Math.log10(distance);

				long upperBound = (long) ((Math.pow(10, vicinity + 1) - distance) / mVicinityMaxSpeeds
						.get(i));
				long lowerBound = (long) ((distance - Math.pow(10, vicinity)) / mVicinityMaxSpeeds
						.get(i));

				minDelay = Math.min(minDelay,
						(Math.min(lowerBound, upperBound)));
				values.put(VICINITY, vicinity);
				putValues(mVicinityIds.get(i), values, now);
			}
			// update for within
			for (int i = 0; i < mWithinLatitudes.size(); i++) {
				ContentValues values = new ContentValues();
				Location.distanceBetween(location.getLatitude(),
						location.getLongitude(), mWithinLatitudes.get(i),
						mWithinLongitudes.get(i), results);
				distance = results[0];
				minDelay = Math
						.min(minDelay,
								(long) (Math.abs((distance - mWithinRanges
										.get(i))) / mWithinMaxSpeeds.get(i)));
				values.put(WITHIN, distance);
				putValues(mWithinIds.get(i), values, now);
			}
			// don't get the value too often if we're close to a point where the
			// value changes
			minDelay = Math.max(minDelay, MIN_TIME_BETWEEN_UPDATES);

			// now sleep for minDelay
			final long sleepTime = minDelay;

			// TODO change this to alarm?
			new Thread() {
				public void run() {
					try {
						sleep(sleepTime * SECOND);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					requestSingleUpdate();
				}
			}.start();
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
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public final String[] getValuePaths() {
		return new String[] { VICINITY, WITHIN };
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
		defaults.putDouble(MAX_SPEED, 28); // 28 m/s ~ 100 km/h
		defaults.putDouble(WITHIN_RANGE, 100); // meter
		defaults.putString(PROVIDER, LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// check whether we can use the advanced methods of API 9+
		try {
			mRequestSingleUpdateMethod = locationManager.getClass().getMethod(
					"requestSingleUpdate",
					new Class[] { String.class, LocationListener.class,
							Looper.class });
		} catch (Throwable t) {
			mRequestSingleUpdateMethod = null;
		}
		// construct the mock location provider (for testing)
		try {
			locationManager.removeTestProvider("test");
		} catch (Throwable t) {
			// ignore
		}
		locationManager
				.addTestProvider("test", false, false, false, false, false,
						false, false, Criteria.POWER_LOW,
						Criteria.ACCURACY_FINE);
		// and enable it
		locationManager.setTestProviderEnabled("test", true);

		// and run the update thread
		mockUpdateThread.start();

	}

	private List<Double> mVicinityMaxSpeeds = new ArrayList<Double>();
	private List<Double> mVicinityLatitudes = new ArrayList<Double>();
	private List<Double> mVicinityLongitudes = new ArrayList<Double>();
	private List<String> mVicinityIds = new ArrayList<String>();

	private List<Double> mWithinMaxSpeeds = new ArrayList<Double>();
	private List<Double> mWithinLatitudes = new ArrayList<Double>();
	private List<Double> mWithinLongitudes = new ArrayList<Double>();
	private List<Double> mWithinRanges = new ArrayList<Double>();
	private List<String> mWithinIds = new ArrayList<String>();

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (valuePath.equals(WITHIN)) {
			mWithinLatitudes.add(Double.valueOf(configuration
					.getDouble(LATITUDE)));
			mWithinLongitudes.add(Double.valueOf(configuration
					.getDouble(LONGITUDE)));
			mWithinMaxSpeeds.add(configuration.getDouble(MAX_SPEED,
					mDefaultConfiguration.getDouble(MAX_SPEED)));
			mWithinRanges.add(configuration.getDouble(WITHIN_RANGE,
					mDefaultConfiguration.getDouble(WITHIN_RANGE)));
			mWithinIds.add(id);
		} else if (valuePath.equals(VICINITY)) {
			mVicinityLatitudes.add(Double.valueOf(configuration
					.getString(LATITUDE)));
			mVicinityLongitudes.add(Double.valueOf(configuration
					.getString(LONGITUDE)));
			mVicinityMaxSpeeds.add(configuration.getDouble(MAX_SPEED,
					mDefaultConfiguration.getDouble(MAX_SPEED)));
			mVicinityIds.add(id);
		} else {
			throw new RuntimeException("invalid valuePath: '" + valuePath
					+ "' for SmartLocationSensor");
		}
		requestSingleUpdate();
	}

	/**
	 * Updates the listener.
	 */
	private void requestSingleUpdate() {
		// stop if we don't have any registerd configurations!
		if (registeredConfigurations.size() == 0) {
			return;
		}

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
				// if it isn't PASSIVE or NETWORK, we can't do any better,
				// it
				// must be GPS
			}
		}
		if (mRequestSingleUpdateMethod != null) {
			requestSingleUpdateGingerBread(mostAccurateProvider);
		} else {

			locationManager.requestLocationUpdates(mostAccurateProvider, 0, 0,
					locationListener, Looper.getMainLooper());
		}
	}

	private void requestSingleUpdateGingerBread(String provider) {
		try {
			mRequestSingleUpdateMethod.invoke(locationManager, new Object[] {
					provider, locationListener, Looper.getMainLooper() });
		} catch (Throwable t) {
			// something went wrong, revert to old method by setting
			// mRequestSingleUpdateMethod to null
			mRequestSingleUpdateMethod = null;
			requestSingleUpdate();
		}
	}

	private void stopListener() {
		locationManager.removeUpdates(locationListener);
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
		}

	}

	@Override
	public void onDestroySensor() {
		mockUpdateThread.interrupt();
		try {
			locationManager.removeTestProvider("test");
		} catch (Throwable t) {
			// ignore
		}
	}

	/**** TESTING CODE FOR MOCK LOCATIONS ***/

	private static final double[][] MOCK_LOCATIONS = new double[][] {
			{ 52.333943, 4.864549 /* VU */},
			{ 52.321983, 4.927613 /* Duivendrecht */},
			{ 52.279711, 5.157254 /* Naarden-Bussum */},
			{ 52.154294, 5.36587 /* Amersfoort */},
			{ 52.154346, 5.922947 /* Apeldoorn */} };

	private static final double MOCK_SPEED = 750; // m/s

	/*
	 * This thread simulates continuous movement between VU and Apeldoorn and
	 * back (with MOCK_SPEED), it updates about every single second.
	 */
	Thread mockUpdateThread = new Thread() {
		public void run() {
			boolean reverse = false;
			int index = 0;
			int next = 0;
			Location location = new Location("test");
			while (!isInterrupted()) {
				next = reverse ? index - 1 : index + 1;
				location.setLatitude(MOCK_LOCATIONS[index][0]);
				location.setLongitude(MOCK_LOCATIONS[index][1]);

				float[] distance = new float[3];
				Location.distanceBetween(MOCK_LOCATIONS[index][0],
						MOCK_LOCATIONS[index][1], MOCK_LOCATIONS[next][0],
						MOCK_LOCATIONS[next][1], distance);

				long timeBetween = (long) (distance[0] / MOCK_SPEED);
				for (int i = 0; i < timeBetween; i++) {
					float indexFraction = (float) i / (float) timeBetween;
					float nextFraction = 1 - indexFraction;
					location.setLatitude(MOCK_LOCATIONS[index][0]
							* indexFraction + MOCK_LOCATIONS[next][0]
							* nextFraction);
					location.setLongitude(MOCK_LOCATIONS[index][1]
							* indexFraction + MOCK_LOCATIONS[next][1]
							* nextFraction);
					locationManager.setTestProviderLocation("test", location);
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						continue;
					}
				}

				if (reverse) {
					index--;
				} else {
					index++;
				}
				// turn around at the end
				if (next == 0 || next == MOCK_LOCATIONS.length - 1) {
					reverse = !reverse;
				}
			}
		}
	};

}
