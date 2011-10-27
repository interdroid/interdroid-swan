package interdroid.contextdroid.contextexpressions;

/**
 * The ways we know how to compare values.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum Comparator implements ParseableEnum<Comparator> {

	/** greater than. */
	GREATER_THAN (0),
	/** less than. */
	LESS_THAN (1),
	/** greater than or equal to. */
	GREATER_THAN_OR_EQUALS (2),
	/** less than or equal to. */
	LESS_THAN_OR_EQUALS (3),
	/** equal to. */
	EQUALS (4),
	/** not equal to. */
	EQUALS_NOT (5),
	/** Regular Expression Match. */
	REGEX_MATCH (6),
	/** String contains. */
	STRING_CONTAINS (7);

	/** The string version of greater than. */
	private static final String GREATER_STRING = ">";
	/** The string verson of less than. */
	private static final String LESS_STRING = "<";
	/** The string version of greater than or equal to.*/
	private static final String GREATER_EQUAL_STRING = ">=";
	/** The string version of less than or equal to. */
	private static final String LESS_EQUAL_STRING = "<=";
	/** The string version of equals. */
	private static final String EQUAL_STRING = "=";
	/** The string version of not equals. */
	private static final String NOT_EQUAL_STRING = "!=";
	/** The string version of regex match. */
	private static final String REGEX_MATCH_STRING = "regex";
	/** The string version of string contains. */
	private static final String STRING_CONTAINS_STRING = "contains";

	/**
	 * The converted value for this enum.
	 */
	private final int mValue;

	/**
	 * Constructs a Comparator.
	 * @param value the convert value
	 */
	private Comparator(final int value) {
		mValue = value;
	}

	/**
	 * The comparators we know about.
	 */
	private static final String[] COMPARATORS = {
		GREATER_STRING, LESS_STRING, GREATER_EQUAL_STRING,
		LESS_EQUAL_STRING, EQUAL_STRING, NOT_EQUAL_STRING,
		REGEX_MATCH_STRING, STRING_CONTAINS_STRING
	};

	/**
	 * The values of the enumeration.
	 */
	private static final Comparator[] VALUES = {
		GREATER_THAN,
		LESS_THAN,
		GREATER_THAN_OR_EQUALS,
		LESS_THAN_OR_EQUALS,
		EQUALS,
		EQUALS_NOT,
		REGEX_MATCH,
		STRING_CONTAINS
	};

	@Override
	public final String toString() {
		return COMPARATORS[convert()];
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public Comparator convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public Comparator parseString(final String val) {
		Comparator ret = null;
		for (int i = 0; i < COMPARATORS.length; i++) {
			if (COMPARATORS[i].equals(val)) {
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
	public static Comparator parse(final String value) {
		return GREATER_THAN.parseString(value);
	}


	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static Comparator convert(final int value) {
		return GREATER_THAN.convertInt(value);
	}
}
