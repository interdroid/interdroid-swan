package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class MovementConfigurationActivity
	extends AbstractConfigurationActivity {

	@Override
	public int getPreferencesXML() {
		return R.xml.movement_preferences;
	}

}
