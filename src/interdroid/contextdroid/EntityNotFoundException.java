package interdroid.contextdroid;

public class EntityNotFoundException extends Exception {

	public EntityNotFoundException(String entityId) {
		super("entity not found: " + entityId);
	}
}
