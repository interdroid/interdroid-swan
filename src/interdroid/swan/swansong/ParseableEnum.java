package interdroid.swan.swansong;

/**
 * This class is designed to make enumerations safe to persist more easily
 * while still allowing us to easily switch on enumerations and
 * also ensures that we can get a string version and return from a string
 * version of the enumeration for representation in expressions where the
 * string doesn't have to match the name of the enumeration value.
 *
 * Enums which implement this should implement static parse and convert methods
 * which returns the enum from the value for convenience. Unfortunately
 * there is now way for interfaces to force a static method and
 * all enumerations extend a single class. This is just a broken part
 * of java enumerations. Fortunately this method is easy to write:
	<code>
	public static E parse(String value) {
		return E.&gt;VALUE&lt;.parseString(value);
	}
	public static E convert(int value) {
		return E.&gt;VALUE&lt;.convertInt(value);
	}
	</code>
 *
 * This encourages the avoidance of using ordinal() to convert an enum
 * to an integer value since they can lead to problems if the enumeration
 * adds valid values at a later date.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 * @param <E>
 */
public interface ParseableEnum<E extends Enum<E>> extends Parseable<E> {

	/**
	 * Converts this enum to an int value.
	 * @return this enum as a safe to persist integer.
	 */
	int convert();

	/**
	 * Converts an int value back to the enum.
	 * @param val the value to convert
	 * @return the matching enumeration
	 */
	E convertInt(int val);

	/* Highly recommended to include these static method.
	public static E parse(String val) {
		return VALUE.parseString(val);
	}

	public static E convert(String val) {
		return VALUE.convertInt(val);
	}
	*/

	/**
	 * @return a parseable version of the enum value.
	 */
	@Override
	String toParseString();
}