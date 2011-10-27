package interdroid.contextdroid.contextexpressions;

import java.util.Scanner;

/**
 * Represents a way to compare expression values.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public enum ComparatorStrategy
implements ParseableEnum<ComparatorStrategy> {

	/** All values are greater than. */
	ALL_GREATER_THAN (0, Comparator.GREATER_THAN, Strategy.ALL),
	/** All values are less than. */
	ALL_LESS_THAN (1, Comparator.LESS_THAN, Strategy.ALL),
	/** All values are greater than or equal to. */
	ALL_GREATER_THAN_OR_EQUALS
	(2, Comparator.GREATER_THAN_OR_EQUALS, Strategy.ALL),
	/** All values are less than or equal to. */
	ALL_LESS_THAN_OR_EQUALS (3, Comparator.LESS_THAN_OR_EQUALS, Strategy.ALL),
	/** All values are equal to. */
	ALL_EQUALS (4, Comparator.EQUALS, Strategy.ALL),
	/** All values are not equal to. */
	ALL_EQUALS_NOT (5, Comparator.EQUALS_NOT, Strategy.ALL),
	/** All values match. */
	ALL_REGEX_MATCH (6, Comparator.REGEX_MATCH, Strategy.ALL),
	/** All values contain. */
	ALL_STRING_CONTAINS (7, Comparator.REGEX_MATCH, Strategy.ALL),


	/** Any value is greater than. */
	ANY_GREATER_THAN (8, Comparator.GREATER_THAN, Strategy.ANY),
	/** Any value is less than. */
	ANY_LESS_THAN (9, Comparator.LESS_THAN, Strategy.ANY),
	/** Any value is greater than or equal to. */
	ANY_GREATER_THAN_OR_EQUALS
	(10, Comparator.GREATER_THAN_OR_EQUALS, Strategy.ANY),
	/** Any value is less than or equal to. */
	ANY_LESS_THAN_OR_EQUALS (11, Comparator.LESS_THAN_OR_EQUALS, Strategy.ANY),
	/** Any value is equal to. */
	ANY_EQUALS (12, Comparator.EQUALS, Strategy.ANY),
	/** Any value is not equal to. */
	ANY_EQUALS_NOT (13, Comparator.EQUALS_NOT, Strategy.ANY),
	/** Any value matches. */
	ANY_REGEX_MATCH (14, Comparator.REGEX_MATCH, Strategy.ANY),
	/** Any string contains. */
	ANY_STRING_CONTAINS (15, Comparator.STRING_CONTAINS, Strategy.ANY);

	/** The comparator this represents. */
	private final Comparator mComparator;

	/** The strategy the comparison takes. */
	private final Strategy mStrategy;

	/** The convert value. */
	private final int mValue;

	/**
	 * All of the enumeration values for quick reference when parsing.
	 * Ugh! This uses implementation details inside Comparator and Strategy
	 * but is better than building a map I think.
	 */
	private static final ComparatorStrategy[][] VALUES =
			new ComparatorStrategy[][]
					{{
						ALL_GREATER_THAN,
						ALL_LESS_THAN,
						ALL_GREATER_THAN_OR_EQUALS,
						ALL_LESS_THAN_OR_EQUALS,
						ALL_EQUALS,
						ALL_EQUALS_NOT,
						ALL_REGEX_MATCH,
						ALL_STRING_CONTAINS
					}, {
						ANY_GREATER_THAN,
						ANY_LESS_THAN,
						ANY_GREATER_THAN_OR_EQUALS,
						ANY_LESS_THAN_OR_EQUALS,
						ANY_EQUALS,
						ANY_EQUALS_NOT,
						ANY_REGEX_MATCH,
						ANY_STRING_CONTAINS
					}};

	/**
	 * Constructs an ExpressionComparator.
	 * @param convert the convert value for this enum
	 * @param compare the comparator for this enum
	 * @param strat the strategy for this enum
	 */
	private ComparatorStrategy(final int convert, final Comparator compare,
			final Strategy strat) {
		mValue = convert;
		mComparator = compare;
		mStrategy = strat;
	}

	@Override
	public String toString() {
		if (mStrategy.equals(Strategy.ALL)) {
			return mComparator.toString();
		} else {
			return mComparator.toString() + " " + mStrategy.toString();
		}
	}

	/**
	 * @return the comparator portion
	 */
	public final Comparator getComparator() {
		return mComparator;
	}

	/**
	 * @return the strategy portion
	 */
	public final Strategy getStrategy() {
		return mStrategy;
	}

	@Override
	public int convert() {
		return mValue;
	}

	@Override
	public ComparatorStrategy convertInt(final int val) {
		ComparatorStrategy ret;
		if (val >= ANY_GREATER_THAN.convert()) {
			ret = VALUES[1][val - ANY_GREATER_THAN.convert()];
		} else {
			ret = VALUES[0][val];
		}
		return ret;
	}

	@Override
	public ComparatorStrategy parseString(final String value) {
		ComparatorStrategy ret = null;

		Scanner scanner = new Scanner(value);
		String compString = scanner.next();
		Comparator comparator = Comparator.parse(compString);
		Strategy strategy = Strategy.ALL;
		if (comparator != null) {
			if (scanner.hasNext()) {
				String strategyString = scanner.next();
				strategy = Strategy.parse(strategyString);
			}
		}
		if (comparator != null && strategy != null) {
			ret = VALUES[strategy.convert()][comparator.convert()];
		}
		return ret;
	}

	/**
	 * Parses a string representation.
	 * @param value the string to parse
	 * @return the expression comparator or null if the parse fails.
	 */
	public static ComparatorStrategy parse(final String value) {
		return ALL_GREATER_THAN.parseString(value);
	}

	/**
	 * Converts a persisted int to the matching enumeration value.
	 * @param value the value to get the enumeration for
	 * @return the enumeration matching this value
	 */
	public static ComparatorStrategy convert(final int value) {
		return ALL_GREATER_THAN.convertInt(value);
	}
}
