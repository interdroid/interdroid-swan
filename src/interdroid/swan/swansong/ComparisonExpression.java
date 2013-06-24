package interdroid.swan.swansong;

import android.content.Context;

public class ComparisonExpression implements TriStateExpression {

	private ValueExpression mLeft;
	private Comparator mComparator;
	private ValueExpression mRight;

	public ComparisonExpression(ValueExpression left, Comparator comparator,
			ValueExpression right) {
		this.mLeft = left;
		this.mComparator = comparator;
		this.mRight = right;
	}

	@Override
	public String toParseString() {
		return mLeft.toParseString() + " " + mComparator.toParseString() + " "
				+ mRight.toParseString();
	}

	public ValueExpression getLeft() {
		return mLeft;
	}

	public ValueExpression getRight() {
		return mRight;
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

	public Comparator getComparator() {
		return mComparator;
	}

	@Override
	public String toCrossDeviceString(Context context, String location) {
		return mLeft.toCrossDeviceString(context, location) + " "
				+ mComparator.toParseString() + " "
				+ mRight.toCrossDeviceString(context, location);
	}

}
