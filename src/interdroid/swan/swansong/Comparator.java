package interdroid.swan.swansong;


/**
 * The ways we know how to compare values.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public enum Comparator implements ParseableEnum<Comparator> {

	/** greater than. */
	GREATER_THAN(0, ">"),
	/** less than. */
	LESS_THAN(1, "<"),
	/** greater than or equal to. */
	GREATER_THAN_OR_EQUALS(2, ">="),
	/** less than or equal to. */
	LESS_THAN_OR_EQUALS(3, "<="),
	/** equal to. */
	EQUALS(4, "=="),
	/** not equal to. */
	NOT_EQUALS(5, "!="),
	/** Regular Expression Match. */
	REGEX_MATCH(6, "regex"),
	/** String contains. */
	STRING_CONTAINS(7, "contains");

	/**
	 * The converted value for this enum.
	 */
	private final int mValue;

	/** The string version of the enum. */
	private String mName;

	/**
	 * Constructs a Comparator.
	 * 
	 * @param value
	 *            the convert value
	 * @param name
	 *            the name of the comparator
	 */
	private Comparator(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public final String toString() {
		return mName;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public Comparator convertInt(final int val) {
		Comparator ret = null;
		for (Comparator comp : Comparator.values()) {
			if (comp.convert() == val) {
				ret = comp;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parses a string and returns a Comparator.
	 * 
	 * @param val
	 *            a string to parse
	 * @return the parsed Comparator
	 */
	private Comparator parseString(final String val) {
		Comparator ret = null;
		for (Comparator comp : Comparator.values()) {
			if (comp.toParseString().equals(val)) {
				ret = comp;
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
	public static Comparator parse(final String value) {
		return GREATER_THAN.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * 
	 * @param value
	 *            the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static Comparator convert(final int value) {
		return GREATER_THAN.convertInt(value);
	}

	@Override
	public String toParseString() {
		return mName;
	}

}
