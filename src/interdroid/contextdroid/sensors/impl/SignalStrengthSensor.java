package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractVdbSensor;
import interdroid.vdb.content.avro.AvroContentProviderProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentValues;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class SignalStrengthSensor extends AbstractVdbSensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(SignalStrengthSensor.class);

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setResult(
					RESULT_OK,
					getIntent().putExtra("configuration",
							SignalStrengthSensor.GSM_SIGNAL_STRENGTH_FIELD));
			finish();
		}

		@Override
		public final int getPreferencesXML() {
			return R.xml.gsm_preferences;
		}

	}

	/**
	 * The gsm signal strength field.
	 */
	public static final String GSM_SIGNAL_STRENGTH_FIELD = "gsm_signal_strength";

	/**
	 * Is this a gsm or cdma reading?
	 */
	public static final String IS_GSM_FIELD = "is_gsm";

	/**
	 * The gsm bit error rate field.
	 */
	public static final String GSM_BIT_ERROR_RATE_FIELD = "gsm_bit_error_rate";

	/**
	 * The cdma dbm field.
	 */
	public static final String CDMA_DBM_FIELD = "cdma_dbm";

	/**
	 * The cdma EC/IO value in dB*10
	 */
	public static final String CDMA_ECIO_FIELD = "cdma_ecio";

	/**
	 * The evdo dbm field.
	 */
	public static final String EVDO_DBM_FIELD = "evdo_dbm";

	/**
	 * The evdo EC/IO value in dB*10
	 */
	public static final String EVDO_ECIO_FIELD = "evdo_ecio";

	/**
	 * The evdo signal to noise field.
	 */
	public static final String EVDO_SNR_FIELD = "evdo_snr";

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
		String scheme = "{'type': 'record', 'name': 'signal', "
				+ "'namespace': 'interdroid.context.sensor.signal',"
				+ "\n'fields': ["
				+ SCHEMA_TIMESTAMP_FIELDS
				+ "\n{'name': '"
				+ IS_GSM_FIELD
				+ "', 'type': 'boolean'},"
				+ "\n{'name': '"
				+ GSM_SIGNAL_STRENGTH_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ GSM_BIT_ERROR_RATE_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ CDMA_DBM_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ CDMA_ECIO_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ EVDO_DBM_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ EVDO_ECIO_FIELD
				+ "', 'type': 'int'},"
				+ "\n{'name': '"
				+ EVDO_SNR_FIELD
				+ "', 'type': 'int'}" + "\n]" + "}";
		return scheme.replace('\'', '"');
	}

	/**
	 * The telephony manager.
	 */
	private TelephonyManager telephonyManager;

	/**
	 * The phone state listener we use.
	 */
	private PhoneStateListener phoneStateListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			long now = System.currentTimeMillis();

			ContentValues values = new ContentValues();

			if (signalStrength.isGsm()) {
				LOG.debug("GSM Signal Strength: {} {}",
						signalStrength.getGsmSignalStrength(),
						signalStrength.getGsmBitErrorRate());
			} else {
				LOG.debug("CDMA Signal Strength: {} {}",
						signalStrength.getCdmaDbm(),
						signalStrength.getCdmaEcio());
			}

			values.put(IS_GSM_FIELD, signalStrength.isGsm());
			values.put(GSM_SIGNAL_STRENGTH_FIELD,
					signalStrength.getGsmSignalStrength());
			values.put(GSM_BIT_ERROR_RATE_FIELD,
					signalStrength.getGsmBitErrorRate());
			values.put(CDMA_DBM_FIELD, signalStrength.getCdmaDbm());
			values.put(CDMA_ECIO_FIELD, signalStrength.getCdmaEcio());
			values.put(EVDO_DBM_FIELD, signalStrength.getEvdoDbm());
			values.put(EVDO_ECIO_FIELD, signalStrength.getEvdoEcio());
			values.put(EVDO_SNR_FIELD, signalStrength.getEvdoSnr());

			putValues(values, now);
		}
	};

	@Override
	public final String[] getValuePaths() {
		return new String[] { IS_GSM_FIELD, GSM_SIGNAL_STRENGTH_FIELD,
				GSM_BIT_ERROR_RATE_FIELD, CDMA_DBM_FIELD, CDMA_ECIO_FIELD,
				EVDO_DBM_FIELD, EVDO_ECIO_FIELD, EVDO_SNR_FIELD };
	}

	@Override
	public final void initDefaultConfiguration(Bundle defaults) {
	}

	@Override
	public final String getScheme() {
		return SCHEME;
	}

	@Override
	public final void onConnected() {
	}

	@Override
	public final void register(final String id, final String valuePath,
			final Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
			telephonyManager.listen(phoneStateListener,
					PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
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
	public final void onDestroySensor() {
		// Nothing to do
	}

}
