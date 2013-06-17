package interdroid.swan.swansong;

import interdroid.swan.crossdevice.Registry;
import android.content.Context;
import android.os.Bundle;

public class SensorValueExpression implements ValueExpression {

	private String mLocation;
	private String mEntity;
	private String mValuePath;
	private Bundle mConfig;
	private HistoryReductionMode mMode;
	private long mHistoryLength;

	public SensorValueExpression(String location, String entity,
			String valuePath, Bundle config, HistoryReductionMode mode,
			long historyLength) {
		mLocation = location;
		mEntity = entity;
		mValuePath = valuePath;
		mConfig = config;
		if (mConfig == null) {
			mConfig = new Bundle();
		}
		mMode = mode;
		mHistoryLength = historyLength;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return mMode;
	}

	public long getHistoryLength() {
		return mHistoryLength;
	}

	@Override
	public String toParseString() {
		String result = mLocation + "@" + mEntity + ":" + mValuePath;
		if (mConfig != null && mConfig.size() > 0) {
			boolean first = true;
			for (String key : mConfig.keySet()) {
				String value = "" + mConfig.get(key);
				if (mConfig.get(key) instanceof String) {
					value = "'" + value + "'";
				}
				result += (first ? "?" : "&") + key + "=" + value;
				first = false;
			}
		}
		result += "{" + mMode.toParseString() + "," + mHistoryLength + "}";
		return result;
	}

	public String getEntity() {
		return mEntity;
	}

	public String getValuePath() {
		return mValuePath;
	}

	public String getLocation() {
		return mLocation;
	}

	public Bundle getConfiguration() {
		return mConfig;
	}

	@Override
	public String toCrossDeviceString(Context context, String location) {
		String result = ((location.equals(mLocation)) ? Expression.LOCATION_SELF
				: Registry.get(context, mLocation))
				+ "@" + mEntity + ":" + mValuePath;
		if (mConfig != null && mConfig.size() > 0) {
			boolean first = true;
			for (String key : mConfig.keySet()) {
				result += (first ? "?" : "&") + key + "="
						+ mConfig.getString(key);
				first = false;
			}
		}
		result += "{" + mMode.toParseString() + "," + mHistoryLength + "}";
		return result;

	}

}