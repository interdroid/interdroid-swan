package interdroid.swan.swansong;

/**
 * Represents the way an expression reduces the history it examines and performs
 * matching.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public enum HistoryReductionMode implements ParseableEnum<HistoryReductionMode> {
	/** No reduction is performed, matching is against all values. */
	ALL(0, "ALL"),
	/** Takes the maximum value. */
	MAX(1, "MAX"),
	/** Takes the minimum value. */
	MIN(2, "MIN"),
	/** Takes the mean value. */
	MEAN(3, "MEAN"),
	/** Takes the median value. */
	MEDIAN(4, "MEDIAN"),
	/** No reduction is performed, matching is against any value. */
	ANY(5, "ANY");

	/** The default HistoryReductionMode for all expressions. */
	public static final HistoryReductionMode DEFAULT_MODE = HistoryReductionMode.ALL;

	/** The convert value. */
	private final int mValue;

	/** The name for the mode. */
	private final String mName;

	/**
	 * Construct with the given convert value.
	 * 
	 * @param value
	 *            the convert value.
	 * @param name
	 *            the name
	 */
	private HistoryReductionMode(final int value, final String name) {
		mValue = value;
		mName = name;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public HistoryReductionMode convertInt(final int val) {
		HistoryReductionMode ret = null;
		for (HistoryReductionMode mode : HistoryReductionMode.values()) {
			if (mode.convert() == val) {
				ret = mode;
				break;
			}
		}
		return ret;
	}

	/**
	 * Parses a string and returns the appropriate mode.
	 * 
	 * @param val
	 *            the string to parse
	 * @return the reduction mode
	 */
	private static HistoryReductionMode parseString(final String val) {
		HistoryReductionMode ret = null;
		for (HistoryReductionMode mode : HistoryReductionMode.values()) {
			if (mode.toParseString().equals(val)) {
				ret = mode;
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
	public static HistoryReductionMode parse(final String value) {
		return HistoryReductionMode.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * 
	 * @param value
	 *            the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static HistoryReductionMode convert(final int value) {
		return ALL.convertInt(value);
	}

	@Override
	public final String toString() {
		return mName;
	}

	@Override
	public final String toParseString() {
		return mName;
	}
}
