package interdroid.contextdroid.sensors;

import android.os.Bundle;
import interdroid.contextdroid.R;

public class GSMSignalStrengthConfigurationActivity extends
		AbstractConfigurationActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(
				RESULT_OK,
				getIntent().putExtra("configuration",
						GSMSignalStrengthSensor.SIGNAL_STRENGTH_FIELD));
		finish();
	}

	@Override
	public int getPreferencesXML() {
		return R.xml.gsm_preferences;
	}

}
