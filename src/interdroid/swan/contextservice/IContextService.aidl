package interdroid.swan.contextservice;

import interdroid.swan.contextexpressions.Expression;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextservice.SwanServiceException;

interface IContextService {

	/**
	 * Adds an expression and keep its state consistent with context knowledge
	 * until it is removed.
	 *
	 * @param expressionTreeWrapper a wrapper for the expression
	 * @param expressionId an identifier to use for checking state and
	 *  removing the expression
	 * @param serviceLevelRequest requested service level settings to use
	 *  while the expression is active
	 */
	SwanServiceException addContextExpression(in String expressionId, in Expression expression);

	/**
	 * Removes a previously added context expression and cancels any
	 * service level request that is bound to it.
	 *
	 * @param expressionId the identifier of the expression
	 */
	SwanServiceException removeContextExpression(String expressionId);

	void notifyDataChanged(in String[] ids);


	SwanServiceException registerContextTypedValue(in String id, in ContextTypedValue contextTypedValue);
	SwanServiceException unregisterContextTypedValue(in String id);

	/**
	 * Attempt to shut down Swan. Doesn't work properly for now.
	 */
	void shutdown();
}
