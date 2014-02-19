package interdroid.swan.engine;

import interdroid.swan.SwanException;

/**
 * Thrown when a sensor fails to be setup properly.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class SensorSetupFailedException extends SwanException {


	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = 4870747041723819862L;

	/**
	 * Constructs this exception.
	 * @param message the message for the exception
	 */
	public SensorSetupFailedException(final String message) {
		super(message);
	}

}
