package interdroid.contextdroid;

import android.os.RemoteException;

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
	public ContextDroidException(String string) {
		super(string);
	}

	/**
	 * Instantiates a new context framework exception.
	 * 
	 * @param e the e
	 */
	public ContextDroidException(RemoteException e) {
		super(e);
	}

}
