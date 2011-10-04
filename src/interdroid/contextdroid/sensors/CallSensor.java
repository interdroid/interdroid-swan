package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.List;

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
			if (values.get(STATE_FIELD).size() >= HISTORY_SIZE) {
				values.get(STATE_FIELD).remove(0);
			}
			values.get(STATE_FIELD).add(new TimestampedValue(state, now, now));
			notifyDataChanged(STATE_FIELD);

			if (values.get(PHONE_NUMBER_FIELD).size() >= HISTORY_SIZE) {
				values.get(PHONE_NUMBER_FIELD).remove(0);
			}
			if (incomingNumber != null && incomingNumber.length() > 0) {
				values.get(PHONE_NUMBER_FIELD).add(
						new TimestampedValue(incomingNumber, now, now));
				notifyDataChanged(PHONE_NUMBER_FIELD);
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

	@Override
	protected List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
