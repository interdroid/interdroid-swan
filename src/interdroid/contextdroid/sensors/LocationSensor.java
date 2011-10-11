package interdroid.contextdroid.sensors;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class LocationSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "LocationSensor";

	public static final String LATITUDE_FIELD = "latitude";
	public static final String LONGITUDE_FIELD = "longitude";
	public static final String ALTITUDE_FIELD = "altitude";
	public static final String SPEED_FIELD = "speed";
	public static final String LOCATION_FIELD = "location";

	public static final String MIN_DISTANCE = "min_distance";
	public static final String MIN_TIME = "min_time";
	public static final String PROVIDER = "provider";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 1000;

	private String currentProvider;

	private LocationManager locationManager;

	/** The location listener. */
	private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			long now = System.currentTimeMillis();
			trimValues(HISTORY_SIZE);
			long expire = now + 10000;
			putValue(LATITUDE_FIELD, now, expire, location.getLatitude());
			putValue(LONGITUDE_FIELD, now, expire, location.getLongitude());
			putValue(ALTITUDE_FIELD, now, expire, location.getAltitude());
			putValue(SPEED_FIELD, now, expire, location.getSpeed());
			putValue(LOCATION_FIELD, now, expire, location);

			System.out.println("new location: " + location);
		}

		public void onProviderDisabled(String provider) {
			Log.d(TAG, "provider disabled: " + provider
					+ " the location sensor uses: " + currentProvider);
			if (provider.equals(currentProvider)) {
				Log.d(TAG,
						"location sensor cannot provide data anymore (disabled)");
			}
		}

		public void onProviderEnabled(String provider) {
			Log.d(TAG, "provider enabled: " + provider);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (provider.equals(currentProvider)
					&& status != LocationProvider.AVAILABLE) {
				Log.d(TAG,
						"location sensor cannot provide data anymore (not available)");
			}

		}
	};

	@Override
	public String[] getValuePaths() {
		return new String[] { LATITUDE_FIELD, LONGITUDE_FIELD, ALTITUDE_FIELD,
				SPEED_FIELD, LOCATION_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(MIN_TIME, 0);
		DEFAULT_CONFIGURATION.putLong(MIN_DISTANCE, 0);
		DEFAULT_CONFIGURATION.putString(PROVIDER,
				LocationManager.NETWORK_PROVIDER);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'location', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ LONGITUDE_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ LATITUDE_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ ALTITUDE_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ SPEED_FIELD
				+ "', 'type': 'double'},"
				+ "            {'name': '"
				+ LOCATION_FIELD
				+ "', 'type': 'location'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() > 0) {
			updateListener();
		}
	}

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
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			locationManager.removeUpdates(locationListener);
		} else {
			updateListener();
		}

	}

}
