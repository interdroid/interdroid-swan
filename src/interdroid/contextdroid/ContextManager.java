package interdroid.contextdroid;

import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.contextdroid.contextservice.ContextDroidServiceException;

import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

/**
 * The Class ContextManager.
 */
public class ContextManager extends ContextServiceConnector {

	/** Logging tag */
	protected static final String TAG = "ContextManager";

	/** The intent action for broadcasts of new readings. */
	public static final String ACTION_NEWREADING = "interdroid.contextdroid.NEWREADING";

	/**
	 * The intent action for announcing the expiration of a the previous reading
	 */
	public static final String ACTION_INVALIDATEREADING = "interdroid.contextdroid.INVALIDATEREADING";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * TRUE.
	 */
	public static final String ACTION_EXPRESSIONTRUE = "interdroid.contextdroid.EXPRESSIONTRUE";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * FALSE.
	 */
	public static final String ACTION_EXPRESSIONFALSE = "interdroid.contextdroid.EXPRESSIONFALSE";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * UNDEFINED.
	 */
	public static final String ACTION_EXPRESSIONUNDEFINED = "interdroid.contextdroid.EXPRESSIONUNDEFINED";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * UNDEFINED.
	 */
	public static final String ACTION_EXPRESSIONERROR = "interdroid.contextdroid.EXPRESSIONERROR";

	/** The Constant TRUE. */
	public static final int TRUE = 1;

	/** The Constant FALSE. */
	public static final int FALSE = 0;

	/** The Constant UNDEFINED. */
	public static final int UNDEFINED = -1;

	/** Stores the listeners for each context expression. */
	private HashMap<String, ContextExpressionListener> contextExpressionListeners;

	/** Has the broadcast receiver for context expressions been registered? */
	private boolean contextExpressionBroadcastReceiverRegistered = false;

	/** Stores the listeners for each context expression. */
	private HashMap<String, ContextTypedValueListener> contextTypedValueListeners;

	/** Has the broadcast receiver for context entities been registered? */
	private boolean contextTypedValueBroadcastReceiverRegistered = false;

	/**
	 * Instantiates a new context manager.
	 * 
	 * @param context
	 *            the application context
	 * @param contextManagerListener
	 *            the ContextManagerListener object to call onConnected() on
	 *            after the context manager has been initialized.
	 */
	public ContextManager(final Context context) {
		super(context);
		contextExpressionListeners = new HashMap<String, ContextExpressionListener>();
		contextTypedValueListeners = new HashMap<String, ContextTypedValueListener>();
	}

	public void destroy() throws ContextDroidException {
		for (String id : contextTypedValueListeners.keySet()) {
			try {
				contextService.unregisterContextTypedValue(id);
			} catch (ContextDroidServiceException e) {
				throw e.getContextDroidException();
			} catch (RemoteException e) {
				throw new ContextDroidException(e);
			} catch (NullPointerException e) {
				throw new ContextServiceNotBoundException(
						"Context Service is not yet bound. Try again later.");
			}
		}
		contextTypedValueListeners.clear();
		for (String id : contextExpressionListeners.keySet()) {
			try {
				contextService.removeContextExpression(id);
			} catch (ContextDroidServiceException e) {
				throw e.getContextDroidException();
			} catch (RemoteException e) {
				throw new ContextDroidException(e);
			} catch (NullPointerException e) {
				throw new ContextServiceNotBoundException(
						"Context Service is not yet bound. Try again later.");
			}
		}
		contextExpressionListeners.clear();
	}

	public void registerContextTypedValue(final String id,
			ContextTypedValue value, ContextTypedValueListener listener)
			throws ContextDroidException {
		try {
			contextService.registerContextTypedValue(id, value);
		} catch (ContextDroidServiceException e) {
			throw e.getContextDroidException();
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		} catch (NullPointerException e) {
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}

		if (listener != null) {
			contextTypedValueListeners.put(id, listener);
		}

		updateContextTypedValueBroadcastReceiver();
	}

	public void unregisterContextTypedValue(final String id)
			throws ContextDroidException {
		try {
			contextService.unregisterContextTypedValue(id);
		} catch (ContextDroidServiceException e) {
			throw e.getContextDroidException();
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		} catch (NullPointerException e) {
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}

		updateContextTypedValueBroadcastReceiver();
	}

	/**
	 * Adds an expression with an explicit identifier. If the identifier already
	 * exists, it will be overwritten.
	 * 
	 * @param expressionId
	 *            the expression id
	 * @param expression
	 *            the expression
	 * @throws ContextDroidException
	 *             the context droid exception
	 */
	public void registerContextExpression(final String expressionId,
			final Expression expression,
			final ContextExpressionListener expressionListener)
			throws ContextDroidException {
		try {
			Log.d(TAG, "attempting to add expression " + expression + " id: "
					+ expressionId);
			contextService.addContextExpression(expressionId, expression);
		} catch (ContextDroidServiceException e) {
			throw e.getContextDroidException();
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		} catch (NullPointerException e) {
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}

		if (expressionListener != null) {
			contextExpressionListeners.put(expressionId, expressionListener);
		}

		updateContextExpressionBroadcastReceiver();
	}

	/**
	 * Removes the context expression.
	 * 
	 * @param expressionId
	 *            the expression id
	 * @throws ContextDroidException
	 *             the context droid exception
	 */
	public void unregisterContextExpression(String expressionId)
			throws ContextDroidException {
		contextExpressionListeners.remove(expressionId);

		updateContextExpressionBroadcastReceiver();

		try {
			contextService.removeContextExpression(expressionId);
		} catch (ContextDroidServiceException e) {
			throw e.getContextDroidException();
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		} catch (NullPointerException e) {
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}
	}

	/*** PRIVATE HELPER FUNCTIONS ***/

	/**
	 * Reregister context expression broadcast receiver. Updates the
	 * IntentFilter of the receiver or removes it altogether if there are no
	 * context expression listeners left.
	 */
	private void updateContextTypedValueBroadcastReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_NEWREADING);
		intentFilter.addAction(ACTION_INVALIDATEREADING);
		intentFilter.addDataScheme("contexttypedvalue");
		for (String id : contextTypedValueListeners.keySet()) {
			intentFilter.addDataAuthority(id, null);
		}
		if (contextTypedValueBroadcastReceiverRegistered) {
			context.unregisterReceiver(contextTypedValueBroadcastReceiver);
			contextTypedValueBroadcastReceiverRegistered = false;
		}
		if (contextExpressionListeners.size() > 0) {
			context.registerReceiver(contextTypedValueBroadcastReceiver,
					intentFilter);
			contextTypedValueBroadcastReceiverRegistered = true;
		}
	}

	/**
	 * Reregister context expression broadcast receiver. Updates the
	 * IntentFilter of the receiver or removes it altogether if there are no
	 * context expression listeners left.
	 */
	private void updateContextExpressionBroadcastReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_EXPRESSIONTRUE);
		intentFilter.addAction(ACTION_EXPRESSIONFALSE);
		intentFilter.addAction(ACTION_EXPRESSIONUNDEFINED);
		intentFilter.addAction(ACTION_EXPRESSIONERROR);
		intentFilter.addDataScheme("contextexpression");
		for (String expressionId : contextExpressionListeners.keySet()) {
			intentFilter.addDataAuthority(expressionId, null);
		}
		if (contextExpressionBroadcastReceiverRegistered) {
			context.unregisterReceiver(contextExpressionBroadcastReceiver);
			contextExpressionBroadcastReceiverRegistered = false;
		}
		if (contextExpressionListeners.size() > 0) {
			context.registerReceiver(contextExpressionBroadcastReceiver,
					intentFilter);
			contextExpressionBroadcastReceiverRegistered = true;
		}
	}

	/**
	 * The receiver object that receives updates about the state of context
	 * expressions.
	 */
	private BroadcastReceiver contextTypedValueBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			Uri data = intent.getData();
			String id = data.getHost();
			ContextTypedValueListener listener = contextTypedValueListeners
					.get(id);

			if (intent.getAction().equals(ACTION_NEWREADING)) {
				if (listener != null) {
					listener.onReading(id, (TimestampedValue[]) intent
							.getExtras().get("values"));
				}
			} else if (intent.getAction().equals(ACTION_INVALIDATEREADING)) {
				if (listener != null) {
					listener.onReading(id, null);
				}
			}
		}

	};

	/**
	 * The receiver object that receives updates about the state of context
	 * expressions.
	 */
	private BroadcastReceiver contextExpressionBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			Uri data = intent.getData();
			String expressionId = data.getHost();
			ContextExpressionListener listener = contextExpressionListeners
					.get(expressionId);

			if (intent.getAction().equals(ACTION_EXPRESSIONTRUE)) {
				if (listener != null) {
					listener.onTrue(expressionId);
				}
			} else if (intent.getAction().equals(ACTION_EXPRESSIONFALSE)) {
				if (listener != null) {
					listener.onFalse(expressionId);
				}
			} else if (intent.getAction().equals(ACTION_EXPRESSIONUNDEFINED)) {
				if (listener != null) {
					listener.onUndefined(expressionId);
				}
			} else if (intent.getAction().equals(ACTION_EXPRESSIONERROR)) {
				if (listener != null) {
					ContextDroidException exception = (ContextDroidException) intent
							.getSerializableExtra("exception");
					listener.onException(expressionId, exception);
				}
			}
		}

	};

}
