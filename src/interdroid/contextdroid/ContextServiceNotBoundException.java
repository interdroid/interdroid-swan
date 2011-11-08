package interdroid.contextdroid;

/**
 * Thrown when the context service is not bound to this application.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ContextServiceNotBoundException extends ContextDroidException {

	/**
	 * Construct an exception with the given message.
	 * @param message the message.
	 */
	public ContextServiceNotBoundException(final String message) {
		super(message);
	}

	/**
	 * the serial version id.
	 */
	private static final long serialVersionUID = -439426237588797428L;

}
