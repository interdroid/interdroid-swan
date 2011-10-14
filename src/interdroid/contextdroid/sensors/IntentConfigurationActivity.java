package interdroid.contextdroid.sensors;

import android.os.Bundle;
import interdroid.contextdroid.R;

public class IntentConfigurationActivity extends AbstractConfigurationActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(
				RESULT_OK,
				getIntent().putExtra("configuration",
						IntentSensor.STARTED_FIELD));
		finish();
	}

	@Override
	public int getPreferencesXML() {
		return R.xml.intent_preferences;
	}

}
