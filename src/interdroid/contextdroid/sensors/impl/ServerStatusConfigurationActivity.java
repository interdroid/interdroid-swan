package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class ServerStatusConfigurationActivity extends
		AbstractConfigurationActivity {

	@Override
	public int getPreferencesXML() {
		return R.xml.serverstatus_preferences;
	}

}
