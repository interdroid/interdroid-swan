package interdroid.contextdroid.contextexpressions;

/**
 * An enumeration which represents LogicalOperators.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum LogicOperator implements ParseableEnum<LogicOperator> {
	/** Logical AND. */
	AND (0, "&&"),
	/** Logical OR. */
	OR (1, "||"),
	/** Logical NOT. */
	NOT (2, "!");

	/** The converted value of this value. */
	private int mValue;

	/** The string version of the enum. */
	private String mName;

	/**
	 * Construct a Logical Operator.
	 * @param value the converted value.
	 * @param name the name of the operator.
	 */
	private LogicOperator(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public LogicOperator convertInt(final int val) {
		LogicOperator ret = null;
		for (LogicOperator op : LogicOperator.values()) {
			if (op.convert() == val) {
				ret = op;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parses and returns a LogicOperation.
	 * @param val the string to parse
	 * @return the corresponding LogicOperation
	 */
	private LogicOperator parseString(final String val) {
		LogicOperator ret = null;
		for (LogicOperator op : LogicOperator.values()) {
			if (op.toParseString().equals(val)) {
				ret = op;
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

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public String toParseString() {
		return mName;
	}
}
