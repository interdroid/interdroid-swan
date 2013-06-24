package interdroid.swan.swansong;

import android.content.Context;

public class MathValueExpression implements ValueExpression {

	private ValueExpression mLeft;
	private ValueExpression mRight;
	private MathOperator mOperator;

	private HistoryReductionMode mMode;

	public MathValueExpression(ValueExpression left, MathOperator operator,
			ValueExpression right, HistoryReductionMode mode) {
		mLeft = left;
		mRight = right;
		mOperator = operator;
		mMode = mode;
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return mMode;
	}

	@Override
	public String toParseString() {
		return "(" + mLeft.toParseString() + " " + mOperator.toParseString()
				+ " " + mRight.toParseString() + ")";
	}

	public ValueExpression getLeft() {
		return mLeft;
	}

	public ValueExpression getRight() {
		return mRight;
	}

	public MathOperator getOperator() {
		return mOperator;
	}

	@Override
	public String getLocation() {
		if (mLeft.getLocation().equals(mRight.getLocation())) {
			// both on same (remote) location
			return mLeft.getLocation();
		} else if (mLeft.getLocation().equals(LOCATION_INDEPENDENT)) {
			// left doesn't care
			return mRight.getLocation();
		} else if (mRight.getLocation().equals(LOCATION_INDEPENDENT)) {
			// right doesn't care
			return mLeft.getLocation();
		} else {
			return LOCATION_SELF;
		}
	}

	@Override
	public String toCrossDeviceString(Context context, String location) {
		return "(" + mLeft.toCrossDeviceString(context, location) + " "
				+ mOperator.toParseString() + " "
				+ mRight.toCrossDeviceString(context, location) + ")";
	}

}
