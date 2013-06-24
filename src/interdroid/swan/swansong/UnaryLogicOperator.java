package interdroid.swan.swansong;


/**
 * An enumeration which represents UnaryLogicalOperators.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public enum UnaryLogicOperator implements ParseableEnum<UnaryLogicOperator>,
		LogicOperator {
	/** Logical NOT. */
	NOT(0, "!");

	/** The converted value of this value. */
	private int mValue;

	/** The string version of the enum. */
	private String mName;

	/**
	 * Construct a UnaryLogical Operator.
	 * 
	 * @param value
	 *            the converted value.
	 * @param name
	 *            the name of the operator.
	 */
	private UnaryLogicOperator(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public UnaryLogicOperator convertInt(final int val) {
		UnaryLogicOperator ret = null;
		for (UnaryLogicOperator op : UnaryLogicOperator.values()) {
			if (op.convert() == val) {
				ret = op;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parses and returns a UnaryLogicOperator.
	 * 
	 * @param val
	 *            the string to parse
	 * @return the corresponding UnaryLogicOperator
	 */
	public UnaryLogicOperator parseString(final String val) {
		UnaryLogicOperator ret = null;
		for (UnaryLogicOperator op : UnaryLogicOperator.values()) {
			if (op.toParseString().equals(val)) {
				ret = op;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parse a string and return the value.
	 * 
	 * @param value
	 *            the value to parse
	 * @return the enum which matches the string.
	 */
	public static UnaryLogicOperator parse(final String value) {
		return NOT.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * 
	 * @param value
	 *            the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static UnaryLogicOperator convert(final int value) {
		return NOT.convertInt(value);
	}

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public String toParseString() {
		return mName;
	}

	@Override
	public TriState operate(TriState first, TriState last) {
		// ignore last result, this is a unary operation
		if (mValue == 0) {
			// NOT
			if (first == TriState.TRUE) {
				return TriState.FALSE;
			} else if (first == TriState.FALSE) {
				return TriState.TRUE;
			} else {
				return TriState.UNDEFINED;
			}
		} else {
			return TriState.UNDEFINED;
		}
	}
}
