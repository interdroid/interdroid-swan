package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class LogCatConfigurationActivity
	extends AbstractConfigurationActivity {

	@Override
	public int getPreferencesXML() {
		return R.xml.logcat_preferences;
	}

}
