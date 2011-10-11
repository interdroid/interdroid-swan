package interdroid.contextdroid;

import interdroid.contextdroid.contextexpressions.TimestampedValue;

/**
 * The listener interface for receiving context events. The class that is
 * concerned with processing a context event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addContextListener<code> method or the 
 * <code>addContextExpressionListener</code>. When the context event occurs,
 * that object's appropriate method is invoked.
 */
public interface ContextTypedValueListener {

	/**
	 * Called when a context expression becomes false
	 * 
	 * @param expressionId
	 *            the expression id
	 * @return
	 */
	void onReading(String id, TimestampedValue[] values);

}
