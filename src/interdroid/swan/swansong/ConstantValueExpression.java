package interdroid.swan.swansong;


public class ConstantValueExpression implements ValueExpression {

	private Result mResult;

	public ConstantValueExpression(Object constant) {
		mResult = new Result(new TimestampedValue[] { new TimestampedValue(
				constant) }, 0);
		mResult.setDeferUntil(Long.MAX_VALUE);
		mResult.setDeferUntilGuaranteed(true);
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
	public void setInferredLocation(String location) {
		throw new RuntimeException(
				"Please don't use this method. For internal use only.");
	}

	@Override
	public String getLocation() {
		return LOCATION_INDEPENDENT;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return HistoryReductionMode.DEFAULT_MODE;
	}

}
