package interdroid.contextdroid;

/**
 * The listener interface for receiving context events. The class that is
 * concerned with processing a context event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addContextListener<code> method or the 
 * <code>addContextExpressionListener</code>. When the context event occurs,
 * that object's appropriate method is invoked.
 */
public interface ContextExpressionListener {

	/**
	 * Called when a context expression becomes false
	 * 
	 * @param expressionId
	 *            the expression id
	 * @return
	 */
	void onFalse(String expressionId);

	/**
	 * Called when a context expression becomes true
	 * 
	 * @param expressionId
	 *            the expression id
	 */
	void onTrue(String expressionId);

	/**
	 * Called when a context expression's value becomes undefined
	 * 
	 * @param expressionId
	 *            the expression id
	 */
	void onUndefined(String expressionId);

	/**
	 * Called when an exception occurs during the evaluation of the expression.
	 * 
	 * @param expressionId
	 * @param exception
	 */
	void onException(String expressionId, ContextDroidException exception);

}
