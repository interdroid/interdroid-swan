package interdroid.contextdroid.sensors.impl;

import android.os.Bundle;
import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class SignalStrengthConfigurationActivity extends
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
	public int getPreferencesXML() {
		return R.xml.gsm_preferences;
	}

}
