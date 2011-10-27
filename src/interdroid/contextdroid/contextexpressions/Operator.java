package interdroid.contextdroid.contextexpressions;

/**
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum Operator implements ParseableEnum<Operator> {
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
	private static final Operator[] VALUES = {
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
	private Operator(final int value) {
		mValue = value;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public Operator convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public Operator parseString(final String val) {
		Operator ret = null;
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
	public static Operator parse(final String value) {
		return MINUS.parseString(value);
	}


	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static Operator convert(final int value) {
		return MINUS.convertInt(value);
	}
}
