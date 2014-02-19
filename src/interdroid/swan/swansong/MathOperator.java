package interdroid.swan.swansong;

/**
 * An enumeration which represents Mathematical Operators.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public enum MathOperator implements ParseableEnum<MathOperator> {
	/** The minus operator. Can be used for Locations as well. */
	MINUS(0, "-"),
	/** The plus operator. */
	PLUS(1, "+"),
	/** The times operator. */
	TIMES(2, "*"),
	/** The divide operator. */
	DIVIDE(3, "/"),
	/** The modulus operator. */
	MOD(4, "%");

	/**
	 * The persistence value of the enum.
	 */
	private final int mValue;

	/** The string version of the enum. */
	private String mName;

	/**
	 * Construct an operator.
	 * 
	 * @param value
	 *            the convert value for the enum
	 * @param name
	 *            the name of the operation
	 */
	private MathOperator(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public MathOperator convertInt(final int val) {
		MathOperator ret = null;
		for (MathOperator op : MathOperator.values()) {
			if (op.convert() == val) {
				ret = op;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parse and return a MathOperator.
	 * 
	 * @param val
	 *            the string to parse
	 * @return the corresponding MathOperator
	 */
	private MathOperator parseString(final String val) {
		MathOperator ret = null;
		for (MathOperator op : MathOperator.values()) {
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
	public static MathOperator parse(final String value) {
		return MINUS.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * 
	 * @param value
	 *            the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static MathOperator convert(final int value) {
		return MINUS.convertInt(value);
	}

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public String toParseString() {
		return mName;
	}
}