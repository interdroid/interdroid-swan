package interdroid.contextdroid.contextexpressions;

/**
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum Strategy implements ParseableEnum<Strategy>, Parseable<Strategy> {

	/** Strategy where all values in the window must match. */
	ALL (0, "ALL"),
	/** Strategy where any value in the window must match. */
	ANY (1, "ANY");

	/** The converted value of this enumeration. */
	private final int mValue;

	/** The parseable name. */
	private final String mName;

	/**
	 * Construct a strategy.
	 * @param value the convert value
	 * @param name the name
	 */
	private Strategy(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public Strategy convertInt(final int val) {
		Strategy ret = null;
		for (Strategy strat : Strategy.values()) {
			if (strat.convert() == val) {
				ret = strat;
			}
		}
		return ret;
	}

	/**
	 * Parse and return a Strategy.
	 * @param value the string to parse
	 * @return the Strategy
	 */
	private Strategy parseString(final String value) {
		Strategy ret = null;
		for (Strategy strat : Strategy.values()) {
			if (strat.toParseString().equals(value)) {
				ret = strat;
			}
		}
		return ret;
	}

	/**
	 * Parse a string and return the value.
	 * @param value the value to parse
	 * @return the enum which matches the string.
	 */
	public static Strategy parse(final String value) {
		return ALL.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static Strategy convert(final int value) {
		return ALL.convertInt(value);
	}

	@Override
	public String toString() {
		return  mName;
	}

	@Override
	public String toParseString() {
		return  mName;
	}

}
