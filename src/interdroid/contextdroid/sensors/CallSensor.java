package interdroid.contextdroid.sensors;

import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "CallSensor";

	public static final String STATE_FIELD = "call_state";
	public static final String PHONE_NUMBER_FIELD = "phone_number";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 0;

	/** The telephony manager. */
	private TelephonyManager telephonyManager;

	private PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			long now = System.currentTimeMillis();
			long expire = now + EXPIRE_TIME;
			trimValues(HISTORY_SIZE);

			putValue(STATE_FIELD, now, expire, state);

			if (incomingNumber != null && incomingNumber.length() > 0) {
				putValue(PHONE_NUMBER_FIELD, now, now, incomingNumber);
			}
		}
	};

	@Override
	public String[] getValuePaths() {
		return new String[] { STATE_FIELD, PHONE_NUMBER_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'call', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ PHONE_NUMBER_FIELD
				+ "', 'type': 'string'},"
				+ "            {'name': '"
				+ STATE_FIELD
				+ "', 'type': 'integer'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		System.out.println("call sensor connected");

	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);
		}
	}

	@Override
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_NONE);
		}
	}

}
