package interdroid.contextdroid.contextexpressions;

/**
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum Strategy implements ParseableEnum<Strategy> {

	/** Strategy where all values in the window must match. */
	ALL (0),
	/** Strategy where any value in the window must match. */
	ANY (1);

	/** The converted value of this enumeration. */
	private final int mValue;

	/**
	 * Construct a strategy.
	 * @param value the convert value
	 */
	private Strategy(final int value) {
		mValue = value;
	}

	/**
	 * All the strategies in this enumeration.
	 */
	private static final Strategy[] VALUES = new Strategy[] {
		ALL, ANY
	};

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public Strategy convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public Strategy parseString(final String value) {
		Strategy ret = null;
		for (int i = 0; i < VALUES.length; i++) {
			if (VALUES[i].name().equals(value)) {
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

}
