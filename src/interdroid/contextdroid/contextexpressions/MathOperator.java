package interdroid.contextdroid.contextexpressions;

/**
 * An enumeration which represents Mathematical Operators.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum MathOperator implements ParseableEnum<MathOperator> {
	/** The minus operator. Can be used for Locations as well. */
	MINUS (0),
	/** The plus operator. */
	PLUS (1),
	/** The times operator. */
	TIMES (2),
	/** The divide operator. */
	DIVIDE (3);

	/**
	 * The persistence value of the enum.
	 */
	private final int mValue;

	/**
	 * The values of this enumeration.
	 */
	private static final MathOperator[] VALUES = {
			MINUS, PLUS, TIMES, DIVIDE
	};

	/** String representing minus. */
	private static final String MINUS_STRING = "-";
	/** String representing plus. */
	private static final String PLUS_STRING = "+";
	/** String representing times. */
	private static final String TIMES_STRING = "*";
	/** String representing divide. */
	private static final String DIVIDE_STRING = "/";

	/** The operators we know of. */
	private static final String[] OPERATORS = new String[] {
		MINUS_STRING, PLUS_STRING, TIMES_STRING, DIVIDE_STRING
	};

	/**
	 * Construct an operator.
	 * @param value the convert value for the enum
	 */
	private MathOperator(final int value) {
		mValue = value;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public MathOperator convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public MathOperator parseString(final String val) {
		MathOperator ret = null;
		for (int i = 0; i < OPERATORS.length; i++) {
			if (OPERATORS[i].equals(val)) {
				ret = VALUES[i];
				break;
			}
		}
		return ret;
	}

	/**
	 * Parse a string and return the value.
	 * @param value the value to parse
	 * @return the enum which matches the string.
	 */
	public static MathOperator parse(final String value) {
		return MINUS.parseString(value);
	}


	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static MathOperator convert(final int value) {
		return MINUS.convertInt(value);
	}
}
