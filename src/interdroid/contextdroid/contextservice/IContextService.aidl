package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextEntityReading;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.ContextEntity;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;

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
	void addContextExpression(in String expressionId, in Expression expression);
		
  /**
   * Removes a previously added context expression and cancels any 
   * service level request that is bound to it.
   * 
   * @param expressionId the identifier of the expression 
   */	
	void removeContextExpression(String expressionId);
	
	void notifyDataChanged(in String[] ids);
	
	
	void registerContextTypedValue(in String id, in ContextTypedValue contextTypedValue);
	void unregisterContextTypedValue(in String id);
	
  /**
   * Attempt to shut down ContextDroid. Doesn't work properly for now. 
   */
	void shutdown();
}
