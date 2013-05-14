package interdroid.swan;

import interdroid.swan.swansong.TimestampedValue;

/**
 * The listener interface for receiving context events. The class that is
 * concerned with processing a context event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addContextListener</code> method or the
 * <code>addContextExpressionListener</code>. When the context event occurs,
 * that object's appropriate method is invoked.
 */
public interface ContextTypedValueListener {

	/**
	 * Called when a context expression becomes false.
	 *
	 * @param id
	 *            the expression id
	 * @param values
	 *            the values which were read
	 */
	void onReading(String id, TimestampedValue[] values);

}
