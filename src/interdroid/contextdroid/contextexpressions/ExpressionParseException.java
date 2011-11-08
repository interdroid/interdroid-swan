package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;

/**
 * An exception which represents a problem with parsing.
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ExpressionParseException extends ContextDroidException {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Construct with an inner exception.
	 * @param e the inner exception.
	 */
	public ExpressionParseException(final Exception e) {
		super(e);
	}
}
