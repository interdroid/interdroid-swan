package interdroid.swan.swansong;


public class ComparisonExpression implements TriStateExpression {

	private ValueExpression mLeft;
	private Comparator mComparator;
	private ValueExpression mRight;
	private String mLocation;

	public ComparisonExpression(String location, ValueExpression left,
			Comparator comparator, ValueExpression right) {
		this.mLocation = location;
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
		return mLocation;
	}

	@Override
	public void setInferredLocation(String location) {
		if (mLocation.equals(Expression.LOCATION_INFER)) {
			mLocation = location;
			return;
		}
		throw new RuntimeException("Trying to set inferred location from '"
				+ mLocation + "' to '" + location
				+ "'. Please don't use this method. For internal use only.");
	}

	public Comparator getComparator() {
		return mComparator;
	}

}
