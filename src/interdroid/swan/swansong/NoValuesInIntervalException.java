package interdroid.swan.swansong;

import interdroid.swan.SwanException;

/**
 * Thrown when an expression has no values in the requested interval.
 *
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class NoValuesInIntervalException extends SwanException {

	/**
	 * Constructor.
	 * @param string the message.
	 */
	public NoValuesInIntervalException(final String string) {
		super(string);
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 6181895992522376091L;

}
