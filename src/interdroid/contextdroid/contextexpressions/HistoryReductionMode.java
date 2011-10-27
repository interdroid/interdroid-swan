package interdroid.contextdroid.contextexpressions;

/**
 * Represents the way an expression reduces the history it examines.
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum HistoryReductionMode
implements ParseableEnum<HistoryReductionMode> {
	/** No reduction is performed. */
	NONE (0),
	/** Takes the maximum value. */
	MAX (1),
	/** Takes the minimum value. */
	MIN (2),
	/** Takes the mean value. */
	MEAN (3),
	/** Takes the median value. */
	MEDIAN (4);

	/** The convert value. */
	private final int mValue;

	/**
	 * Construct with the given convert value.
	 * @param value the convert value.
	 */
	private HistoryReductionMode(final int value) {
		mValue = value;
	}

	/** The value of this enumeration. */
	private static final HistoryReductionMode[] VALUES = {
		NONE, MAX, MIN, MEAN, MEDIAN
	};

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public HistoryReductionMode convertInt(final int val) {
		return VALUES[val];
	}

	@Override
	public HistoryReductionMode parseString(final String val) {
		HistoryReductionMode ret = null;
		for (int i = 0; i < VALUES.length; i++) {
			if (VALUES[i].name().equals(val)) {
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
	public static HistoryReductionMode parse(final String value) {
		return NONE.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static HistoryReductionMode convert(final int value) {
		return NONE.convertInt(value);
	}
}
