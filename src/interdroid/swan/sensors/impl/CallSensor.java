package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;
import android.content.ContentValues;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Sensor for phone state.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class CallSensor extends AbstractVdbSensor {

	private static final String TAG = "Call Sensor";

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
			return R.xml.call_preferences;
		}

	}

	/**
	 * The call state.
	 */
	public static final String STATE_FIELD = "call_state";

	/**
	 * The phone number associated with the state if any.
	 */
	public static final String PHONE_NUMBER_FIELD = "phone_number";

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
		String scheme = "{'type': 'record', 'name': 'call', "
				+ "'namespace': 'interdroid.context.sensor.call',"
				+ "\n'fields': [" + SCHEMA_TIMESTAMP_FIELDS + "\n{'name': '"
				+ STATE_FIELD + "', 'type': 'int'}," + "\n{'name': '"
				+ PHONE_NUMBER_FIELD + "', 'type': 'string'}" + "\n]" + "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The telephony manager we use.
	 * */
	private TelephonyManager telephonyManager;

	/**
	 * The phone state listener which gets notified on call state changed.
	 */
	private PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(final int state,
				final String incomingNumber) {
			Log.d(TAG, "Call State: " + state + " " + incomingNumber);

			long now = System.currentTimeMillis();

			ContentValues values = new ContentValues();
			values.put(STATE_FIELD, state);

			if (incomingNumber != null && incomingNumber.length() > 0) {
				values.put(PHONE_NUMBER_FIELD, incomingNumber);
			}

			putValues(values, now);
		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { STATE_FIELD, PHONE_NUMBER_FIELD };
	}

	@Override
	public final void initDefaultConfiguration(final Bundle defaults) {
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
		Log.d(TAG, "call sensor connected");
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	@Override
	public final void unregister(final String id) {
		if (registeredConfigurations.size() == 0) {
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_NONE);
		}
	}

	@Override
	public void onDestroySensor() {
		// TODO Auto-generated method stub

	}

}
