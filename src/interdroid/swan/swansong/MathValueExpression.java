package interdroid.swan.swansong;


public class MathValueExpression implements ValueExpression {

	private ValueExpression mLeft;
	private ValueExpression mRight;
	private MathOperator mOperator;
	private String mLocation;

	private HistoryReductionMode mMode;

	public MathValueExpression(String location, ValueExpression left,
			MathOperator operator, ValueExpression right,
			HistoryReductionMode mode) {
		mLocation = location;
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
		return mLocation;
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

}
