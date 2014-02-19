package interdroid.swan.swansong;


public class LogicExpression implements TriStateExpression {

	private TriStateExpression mLeft;
	private TriStateExpression mRight;
	private LogicOperator mOperator;
	private String mLocation;

	private LogicExpression(String location, TriStateExpression left,
			LogicOperator operator, TriStateExpression right) {
		this.mLocation = location;
		this.mLeft = left;
		this.mRight = right;
		this.mOperator = operator;
	}

	public LogicExpression(String location, TriStateExpression left,
			BinaryLogicOperator operator, TriStateExpression right) {
		this(location, left, (LogicOperator) operator, right);
	}

	public LogicExpression(String location, UnaryLogicOperator operator,
			TriStateExpression expression) {
		this(location, expression, operator, null);
	}

	@Override
	public void setInferredLocation(String location) {
		if (mLocation.equals(Expression.LOCATION_INFER)) {
			mLocation = location;
			return;
		}
		throw new RuntimeException(
				"Please don't use this method. For internal use only.");
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
		return mLocation;
	}

}
