package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class BatteryConfigurationActivity extends AbstractConfigurationActivity {

	@Override
	public int getPreferencesXML() {
		return R.xml.battery_preferences;
	}

}
