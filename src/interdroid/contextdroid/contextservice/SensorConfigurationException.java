package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextDroidException;

/**
 * Thrown when a sensor is not configured properly.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class SensorConfigurationException extends ContextDroidException {

	/**
	 * The serial version id.
	 */
	private static final long serialVersionUID = -252249549122493864L;

	/**
	 * Construct this type of exception.
	 * @param message the message for the exception
	 */
	public SensorConfigurationException(final String message) {
		super(message);
	}

}
