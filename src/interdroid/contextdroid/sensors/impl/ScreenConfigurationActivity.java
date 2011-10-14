package interdroid.contextdroid.sensors;

import android.os.Bundle;
import interdroid.contextdroid.R;

public class ScreenConfigurationActivity extends AbstractConfigurationActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(
				RESULT_OK,
				getIntent().putExtra("configuration",
						ScreenSensor.IS_SCREEN_ON_FIELD));
		finish();
	}

	@Override
	public int getPreferencesXML() {
		return R.xml.screen_preferences;
	}

}
