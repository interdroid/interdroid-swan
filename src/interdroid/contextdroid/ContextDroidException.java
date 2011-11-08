package interdroid.contextdroid;


/**
 * Exception thrown by the ContextManager.
 */
public class ContextDroidException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 330707408532036445L;

	/**
	 * Instantiates a new context framework exception.
	 *
	 * @param string the string
	 */
	public ContextDroidException(final String string) {
		super(string);
	}

	/**
	 * Instantiates a new context framework exception.
	 *
	 * @param e the e
	 */
	public ContextDroidException(final Exception e) {
		super(e);
	}

}
