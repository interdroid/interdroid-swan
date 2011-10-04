package interdroid.contextdroid;

/**
 * The listener interface for applications to detect when the ContextService
 * becomes available for use.
 * 
 */
public interface ConnectionListener {

	/**
	 * Called when the connection to the Context Service has been set up. Any
	 * calls that require the Context Service should be done after this.
	 */
	void onConnected();

	/**
	 * Called when the connection to the Context Service is somehow lost.
	 */
	void onDisconnected();
}