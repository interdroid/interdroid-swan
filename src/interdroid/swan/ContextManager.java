package interdroid.swan;

import interdroid.swan.contextservice.SwanServiceException;
import interdroid.swan.swansong.ContextTypedValue;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.TimestampedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * The Class ContextManager.
 */
public class ContextManager extends ContextServiceConnector {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(ContextManager.class);

	/** The intent action for broadcasts of new readings. */
	public static final String ACTION_NEWREADING =
			"interdroid.swan.NEWREADING";

	/**
	 * The intent action for announcing the expiration of the previous reading.
	 */
	public static final String ACTION_INVALIDATEREADING =
			"interdroid.swan.INVALIDATEREADING";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * TRUE.
	 */
	public static final String ACTION_EXPRESSIONTRUE =
			"interdroid.swan.EXPRESSIONTRUE";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * FALSE.
	 */
	public static final String ACTION_EXPRESSIONFALSE =
			"interdroid.swan.EXPRESSIONFALSE";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * UNDEFINED.
	 */
	public static final String ACTION_EXPRESSIONUNDEFINED =
			"interdroid.swan.EXPRESSIONUNDEFINED";

	/**
	 * The intent action for broadcasts of expressions changing their state to
	 * UNDEFINED.
	 */
	public static final String ACTION_EXPRESSIONERROR =
			"interdroid.swan.EXPRESSIONERROR";

	/** The Constant TRUE. */
	public static final int TRUE = 1;

	/** The Constant FALSE. */
	public static final int FALSE = 0;

	/** The Constant UNDEFINED. */
	public static final int UNDEFINED = -1;

	/** Stores the listeners for each context expression. */
	private final HashMap<String, ContextExpressionListener>
	contextExpressionListeners;

	/** Has the broadcast receiver for context expressions been registered? */
	private boolean contextExpressionBroadcastReceiverRegistered = false;

	/** Stores the listeners for each context expression. */
	private final HashMap<String, ContextTypedValueListener>
	contextTypedValueListeners;

	/** Has the broadcast receiver for context entities been registered? */
	private boolean contextTypedValueBroadcastReceiverRegistered = false;

	/**
	 * Instantiates a new context manager.
	 *
	 * @param context
	 *            the application context
	 */
	public ContextManager(final Context context) {
		super(context);
		contextExpressionListeners =
				new HashMap<String, ContextExpressionListener>();
		contextTypedValueListeners =
				new HashMap<String, ContextTypedValueListener>();
	}

	/**
	 * Destroy this ContextManager. This should be called in
	 * <code>onStop</code> of the activity that started this ContextManager.
	 * @throws SwanException if something goes wrong.
	 */
	public final void destroy() throws SwanException {
		for (String id : contextTypedValueListeners.keySet()) {
			try {
				SwanServiceException exception = getContextService()
						.unregisterContextTypedValue(id);
				if (exception != null) {
					throw exception.getSwanException();
				}
			} catch (RemoteException e) {
				throw new SwanException(e);
			} catch (NullPointerException e) {
				throw new ContextServiceNotBoundException(
						"Context Service is not yet bound. Try again later.");
			}
		}
		contextTypedValueListeners.clear();
		for (String id : contextExpressionListeners.keySet()) {
			try {
				SwanServiceException exception = getContextService()
						.removeContextExpression(id);
				if (exception != null) {
					throw exception.getSwanException();
				}
			} catch (RemoteException e) {
				throw new SwanException(e);
			} catch (NullPointerException e) {
				throw new ContextServiceNotBoundException(
						"Context Service is not yet bound. Try again later.");
			}
		}
		contextExpressionListeners.clear();
	}

	/**
	 * Registers a context typed value with the system.
	 * @param id the id for the value.
	 * @param value the value to register.
	 * @param listener the listener for callbacks for the value.
	 * @throws SwanException if registration fails.
	 */
	public final void registerContextTypedValue(final String id,
			final ContextTypedValue value,
			final ContextTypedValueListener listener)
					throws SwanException {
		try {
			SwanServiceException exception = getContextService()
					.registerContextTypedValue(id, value);
			if (exception != null) {
				throw exception.getSwanException();
			}
		} catch (RemoteException e) {
			throw new SwanException(e);
		} catch (NullPointerException e) {
			LOG.debug("Null pointer", e);
			LOG.debug("service: {}", getContextService());
			LOG.debug("id: {} value: {}", id, value);
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}

		if (listener != null) {
			contextTypedValueListeners.put(id, listener);
		}

		updateContextTypedValueBroadcastReceiver();
	}

	/**
	 * Unregisters the ContextTypedValue with the given id.
	 * @param id the id of the TypedValue to unregister
	 * @throws SwanException if something goes wrong.
	 */
	public final void unregisterContextTypedValue(final String id)
			throws SwanException {
		try {
			SwanServiceException exception = getContextService()
					.unregisterContextTypedValue(id);
			if (exception != null) {
				throw exception.getSwanException();
			}
		} catch (RemoteException e) {
			throw new SwanException(e);
		} catch (NullPointerException e) {
			LOG.debug("Null Pointer", e);
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
	 * @param expressionListener
	 *            the listener for expression events
	 * @throws SwanException
	 *             the context droid exception
	 */
	public final void registerContextExpression(final String expressionId,
			final Expression expression,
			final ContextExpressionListener expressionListener)
					throws SwanException {
		try {
			LOG.debug("attempting to add expression {} id: {}",
					expression, expressionId);
			SwanServiceException exception = getContextService()
					.addContextExpression(expressionId, expression);
			if (exception != null) {
				throw exception.getSwanException();
			}
		} catch (RemoteException e) {
			throw new SwanException(e);
		} catch (NullPointerException e) {
			LOG.debug("Got null pointer.", e);
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
	 * @throws SwanException
	 *             the context droid exception
	 */
	public final void unregisterContextExpression(final String expressionId)
			throws SwanException {
		contextExpressionListeners.remove(expressionId);

		updateContextExpressionBroadcastReceiver();

		try {
			SwanServiceException exception = getContextService()
					.removeContextExpression(expressionId);
			if (exception != null) {
				throw exception.getSwanException();
			}
		} catch (RemoteException e) {
			throw new SwanException(e);
		} catch (NullPointerException e) {
			LOG.debug("Got null pointer.", e);
			throw new ContextServiceNotBoundException(
					"Context Service is not yet bound. Try again later.");
		}
	}

	/*** PRIVATE HELPER FUNCTIONS ***/

	/**
	 * Re-register context expression broadcast receiver. Updates the
	 * IntentFilter of the receiver or removes it altogether if there are no
	 * context expression listeners left.
	 */
	private void updateContextTypedValueBroadcastReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_NEWREADING);
		intentFilter.addAction(ACTION_INVALIDATEREADING);
		intentFilter.addDataScheme("contextvalues");
		for (String id : contextTypedValueListeners.keySet()) {
			intentFilter.addDataAuthority(id, null);
		}
		if (contextTypedValueBroadcastReceiverRegistered) {
			getContext().unregisterReceiver(contextTypedValueBroadcastReceiver);
			contextTypedValueBroadcastReceiverRegistered = false;
		}
		if (contextTypedValueListeners.size() > 0) {
			getContext().registerReceiver(contextTypedValueBroadcastReceiver,
					intentFilter);
			contextTypedValueBroadcastReceiverRegistered = true;
		}
	}

	/**
	 * Re-register context expression broadcast receiver. Updates the
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
			getContext().unregisterReceiver(contextExpressionBroadcastReceiver);
			contextExpressionBroadcastReceiverRegistered = false;
		}
		if (contextExpressionListeners.size() > 0) {
			getContext().registerReceiver(contextExpressionBroadcastReceiver,
					intentFilter);
			contextExpressionBroadcastReceiverRegistered = true;
		}
	}

	/**
	 * The receiver object that receives updates about the state of context
	 * expressions.
	 */
	private final BroadcastReceiver contextTypedValueBroadcastReceiver
	= new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			Uri data = intent.getData();
			String id = data.getHost();
			ContextTypedValueListener listener = contextTypedValueListeners
					.get(id);

			if (intent.getAction().equals(ACTION_NEWREADING)) {
				if (listener != null) {
					Parcelable[] parcelables = (Parcelable[]) intent
							.getExtras().get("values");
					TimestampedValue[] timestampedValues =
							new TimestampedValue[parcelables.length];
					System.arraycopy(parcelables, 0, timestampedValues, 0,
							parcelables.length);

					listener.onReading(id, timestampedValues);
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
	private final BroadcastReceiver contextExpressionBroadcastReceiver =
			new BroadcastReceiver() {
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
					SwanException exception =
							(SwanException) intent
							.getSerializableExtra("exception");
					listener.onException(expressionId, exception);
				}
			}
		}

	};

	/**
	 * @param context the context to use to fetch sensor information
	 * @return a list of SensorServiceInfo with information about sensors.
	 */
	public static List<SensorServiceInfo> getSensors(final Context context) {
		List<SensorServiceInfo> result = new ArrayList<SensorServiceInfo>();
		LOG.debug("Starting sensor discovery");
		PackageManager pm = context.getPackageManager();
		Intent queryIntent = new Intent(
				"interdroid.swan.sensor.DISCOVER");
		List<ResolveInfo> discoveredSensors = pm.queryIntentServices(
				queryIntent, PackageManager.GET_META_DATA);
		LOG.debug("Found " + discoveredSensors.size() + " sensors");
		for (ResolveInfo discoveredSensor : discoveredSensors) {
			try {
				LOG.debug("\tDiscovered sensor: {} {}",
						discoveredSensor.serviceInfo.packageName,
						discoveredSensor.serviceInfo.name);
				result.add(new SensorServiceInfo(new ComponentName(
						discoveredSensor.serviceInfo.packageName,
						discoveredSensor.serviceInfo.name),
						discoveredSensor.serviceInfo.metaData));
			} catch (Exception e) {
				LOG.error("Error with discovered sensor: {}", discoveredSensor);
				LOG.error("Exception", e);
			}
		}
		return result;
	}

}
