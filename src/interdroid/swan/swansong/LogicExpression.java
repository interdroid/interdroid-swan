package interdroid.swan.swansong;

import android.content.Context;

public class LogicExpression implements TriStateExpression {

	private TriStateExpression mLeft;
	private TriStateExpression mRight;
	private LogicOperator mOperator;

	public LogicExpression(TriStateExpression left,
			BinaryLogicOperator operator, TriStateExpression right) {
		this.mLeft = left;
		this.mRight = right;
		this.mOperator = operator;
	}

	public LogicExpression(UnaryLogicOperator operator,
			TriStateExpression expression) {
		this.mLeft = expression;
		this.mOperator = operator;
	}

	public TriStateExpression getLeft() {
		return mLeft;
	}

	public TriStateExpression getRight() {
		return mRight;
	}

	public TriStateExpression getFirst(boolean leftFirst) {
		if (leftFirst) {
			return mLeft;
		} else {
			return mRight;
		}
	}

	public TriStateExpression getLast(boolean leftFirst) {
		if (leftFirst) {
			return mRight;
		} else {
			return mLeft;
		}
	}

	public LogicOperator getOperator() {
		return mOperator;
	}

	@Override
	public String toParseString() {
		if (mRight == null) {
			return mOperator.toString() + " " + mLeft.toParseString();
		} else {
			return "(" + mLeft.toParseString() + " " + mOperator + " "
					+ mRight.toParseString() + ")";
		}
	}

	@Override
	public String getLocation() {
		if (mRight == null) {
			// unary logic
			return mLeft.getLocation();
		} else if (mLeft.getLocation().equals(LOCATION_SELF)
				|| mRight.getLocation().equals(LOCATION_SELF)) {
			// one of the children is local, so this expression is also local
			return LOCATION_SELF;
		} else if (mLeft.getLocation().equals(mRight.getLocation())) {
			// both on same (remote) location
			return mLeft.getLocation();
		} else if (mLeft.getLocation().equals(LOCATION_INDEPENDENT)) {
			// left doesn't care
			return mRight.getLocation();
		} else if (mRight.getLocation().equals(LOCATION_INDEPENDENT)) {
			// right doesn't care
			return mLeft.getLocation();
		} else {
			// TODO do this
			throw new RuntimeException("TODO!");
		}
	}

	@Override
	public String toCrossDeviceString(Context context, String location) {
		if (mRight == null) {
			return mOperator.toString() + " "
					+ mLeft.toCrossDeviceString(context, location);
		} else {
			return "(" + mLeft.toCrossDeviceString(context, location) + " "
					+ mOperator + " "
					+ mRight.toCrossDeviceString(context, location) + ")";
		}
	}
}
