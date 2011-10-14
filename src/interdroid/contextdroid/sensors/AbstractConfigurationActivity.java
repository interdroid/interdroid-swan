package interdroid.contextdroid.sensors;

import java.util.Map;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

public abstract class AbstractConfigurationActivity extends PreferenceActivity
		implements OnPreferenceChangeListener {

	public abstract int getPreferencesXML();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(getPreferencesXML());
		setupPrefs();
		setResult(RESULT_CANCELED);
	}

	private void setupPrefs() {
		setupPref(getPreferenceScreen());
	}

	private void setupPref(Preference preference) {
		if (preference instanceof PreferenceGroup) {
			for (int i = 0; i < ((PreferenceGroup) preference)
					.getPreferenceCount(); i++) {
				// setup all sub prefs
				setupPref(((PreferenceGroup) preference).getPreference(i));
			}
		} else {
			// setup the listener
			preference.setOnPreferenceChangeListener(this);
			// set the summary
			String summary = null;
			if (preference instanceof ListPreference) {
				try {
					summary = ((ListPreference) preference).getValue()
							.toString();
				} catch (NullPointerException e) {
					// ignore
				}
			} else if (preference instanceof EditTextPreference) {
				summary = ((EditTextPreference) preference).getText();
			}
			if (summary != null) {
				preference.setSummary(summary);
			}

			if (preference instanceof ListPreference) {
				if (((ListPreference) preference).getEntries().length == 1) {
					preference.setEnabled(false);
				}
				((ListPreference) preference)
						.setValue(((ListPreference) preference)
								.getEntryValues()[0].toString());
				preference.setSummary(((ListPreference) preference)
						.getEntries()[0]);
			}

		}
	}

	@Override
	public void onBackPressed() {
		setResult(
				RESULT_OK,
				getIntent().putExtra("configuration",
						prefsToConfigurationString()));
		finish();
	}

	private String prefsToConfigurationString() {
		Map<String, ?> map = PreferenceManager.getDefaultSharedPreferences(
				getBaseContext()).getAll();
		String result = map.remove("valuepath").toString() + "?";
		for (String key : map.keySet()) {
			result += key + "=" + map.get(key).toString() + "&";
		}
		return result.substring(0, result.length() - 1);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference instanceof ListPreference) {
			for (int i = 0; i < ((ListPreference) preference).getEntryValues().length; i++) {
				if (((ListPreference) preference).getEntryValues()[i]
						.toString().equals(newValue.toString()
								)) {
					preference.setSummary(((ListPreference) preference)
							.getEntries()[i]);
					return true;
				}
			}

		} else {
			preference.setSummary(newValue.toString());
		}
		return true;
	}

}
