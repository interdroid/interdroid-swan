package interdroid.swan.swansong;

import android.content.Context;

public class ConstantValueExpression implements ValueExpression {

	private Result mResult;

	public ConstantValueExpression(Object constant) {
		mResult = new Result(new TimestampedValue[] { new TimestampedValue(
				constant) });
		mResult.setDeferUntil(Long.MAX_VALUE);
	}

	public Result getResult() {
		return mResult;
	}

	@Override
	public String toParseString() {
		if (mResult.mValues[0].getValue() instanceof String) {
			return "'" + mResult.mValues[0].getValue().toString() + "'";
		} else {
			return mResult.mValues[0].getValue().toString();
		}
	}

	@Override
	public String getLocation() {
		return LOCATION_INDEPENDENT;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return HistoryReductionMode.DEFAULT_MODE;
	}

	@Override
	public String toCrossDeviceString(Context context, String location) {
		return toParseString();
	}
}
