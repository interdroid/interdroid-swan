package interdroid.contextdroid.contextexpressions;

/**
 * An enumeration which represents LogicalOperators.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum LogicOperator implements ParseableEnum<LogicOperator> {
	/** Logical AND. */
	AND (0),
	/** Logical OR. */
	OR (1),
	/** Logical NOT. */
	NOT (3);

	/** The converted value of this value. */
	private int mValue;

	/**
	 * Construct a Logical Operator.
	 * @param value the converted value.
	 */
	private LogicOperator(final int value) {
		mValue = value;
	}


	/**
	 * The values of this enumeration.
	 */
	private static final LogicOperator[] VALUES = {
			AND, OR, NOT
	};

	/** String representing minus. */
	private static final String AND_STRING = "&&";
	/** String representing plus. */
	private static final String OR_STRING = "||";
	/** String representing times. */
	private static final String NOT_STRING = "!";

	/** The operators we know of. */
	private static final String[] OPERATORS = new String[] {
		AND_STRING, OR_STRING, NOT_STRING
	};

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public LogicOperator convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public LogicOperator parseString(final String val) {
		LogicOperator ret = null;
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
	public static LogicOperator parse(final String value) {
		return AND.parseString(value);
	}


	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static LogicOperator convert(final int value) {
		return AND.convertInt(value);
	}
}
