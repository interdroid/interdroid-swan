package interdroid.swan.swansong;


/**
 * An enumeration which represents BinaryLogicalOperators.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public enum BinaryLogicOperator implements ParseableEnum<BinaryLogicOperator>,
		LogicOperator {
	/** Logical AND. */
	AND(0, "&&"),
	/** Logical OR. */
	OR(1, "||");

	/** The converted value of this value. */
	private int mValue;

	/** The string version of the enum. */
	private String mName;

	/**
	 * Construct a BinaryLogical Operator.
	 * 
	 * @param value
	 *            the converted value.
	 * @param name
	 *            the name of the operator.
	 */
	private BinaryLogicOperator(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public BinaryLogicOperator convertInt(final int val) {
		BinaryLogicOperator ret = null;
		for (BinaryLogicOperator op : BinaryLogicOperator.values()) {
			if (op.convert() == val) {
				ret = op;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parses and returns a BinaryLogicOperator.
	 * 
	 * @param val
	 *            the string to parse
	 * @return the corresponding BinaryLogicOperator
	 */
	public BinaryLogicOperator parseString(final String val) {
		BinaryLogicOperator ret = null;
		for (BinaryLogicOperator op : BinaryLogicOperator.values()) {
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
	public static BinaryLogicOperator parse(final String value) {
		return AND.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * 
	 * @param value
	 *            the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static BinaryLogicOperator convert(final int value) {
		return AND.convertInt(value);
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
		if (mValue == 0) {
			// AND
			if (first == TriState.TRUE && last == TriState.TRUE) {
				return TriState.TRUE;
			} else if (first == TriState.UNDEFINED || last == TriState.UNDEFINED) {
				return TriState.UNDEFINED;
			} else {
				return TriState.FALSE;
			}
		} else if (mValue == 1) {
			// OR
			if (first == TriState.UNDEFINED && last == TriState.UNDEFINED) {
				return TriState.UNDEFINED;
			} else if (first == TriState.TRUE || last == TriState.TRUE) {
				return TriState.TRUE;
			} else {
				return TriState.FALSE;
			}
		} else {
			return TriState.UNDEFINED;
		}
	}
}
