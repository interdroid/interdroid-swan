package interdroid.contextdroid.sensors;

import android.app.Activity;
import android.os.Bundle;

public class TimeConfigurationActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(
				RESULT_OK,
				getIntent().putExtra("configuration",
						TimeSensor.CURRENT_MS_FIELD));
		finish();
	}

}
