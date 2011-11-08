package interdroid.contextdroid;

/**
 * Thrown when an entity is not found within ContextDroid, that is
 * a sensor can not be found for a given expression.
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class EntityNotFoundException extends Exception {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Constructor which takes an entity id.
	 * @param entityId The id of the entity
	 */
	public EntityNotFoundException(final String entityId) {
		super("entity not found: " + entityId);
	}
}
