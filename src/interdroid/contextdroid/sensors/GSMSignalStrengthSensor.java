package interdroid.contextdroid.sensors;

import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class GSMSignalStrengthSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "GSMSignalStrengthSensor";

	public static final String SIGNAL_STRENGTH_FIELD = "signal_strength";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 0;

	/** The telephony manager. */
	private TelephonyManager telephonyManager;

	private PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			long now = System.currentTimeMillis();

			trimValues(HISTORY_SIZE);
			putValue(SIGNAL_STRENGTH_FIELD, now, now,
					signalStrength.getGsmSignalStrength());

			System.out.println("new signal strength: "
					+ signalStrength.getGsmSignalStrength());
		}
	};

	@Override
	public String[] getValuePaths() {
		return new String[] { SIGNAL_STRENGTH_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'gsm', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ SIGNAL_STRENGTH_FIELD
				+ "', 'type': 'integer'}"
				+ "           ]" + "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
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
