package interdroid.swan;

import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriState;
import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.ValueExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcelable;
import android.util.Log;

public class ExpressionManager {

	private static final String TAG = "ExpressionManager";

	/**
	 * Action to be used to register an {@link Expression} with an
	 * {@link Intent}. Registration should preferably be done with the
	 * {@link #registerExpression(Context, String, Expression, ExpressionListener)}
	 * ,
	 * {@link #registerTriStateExpression(Context, String, TriStateExpression, TriStateExpressionListener)}
	 * ,
	 * {@link #registerValueExpression(Context, String, ValueExpression, ValueExpressionListener)}
	 * convenience methods.
	 */
	public static final String ACTION_REGISTER = "interdroid.swan.REGISTER";

	/**
	 * Action to be used to unregister an {@link Expression} with an
	 * {@link Intent}. Unregistration should preferably be done with the
	 * {@link #unregisterExpression(Context, String)} convenience method.
	 */
	public static final String ACTION_UNREGISTER = "interdroid.swan.UNREGISTER";

	/**
	 * Action to filter on with a broadcast receiver that indicates the arrival
	 * of new values for a {@link ValueExpression}. The
	 * {@link ValueExpressionListener} already listens to these broadcasts and
	 * forwards them to the listener.
	 */
	public static final String ACTION_NEW_VALUES = "interdroid.swan.NEW_VALUES";

	/**
	 * Action to filter on with a broadcast receiver that indicates the arrival
	 * of a new tristate for a {@link TristateExpression}. The
	 * {@link TristateExpressionListener} already listens to these broadcasts
	 * and forwards them to the listener.
	 */
	public static final String ACTION_NEW_TRISTATE = "interdroid.swan.NEW_TRISTATE";

	/**
	 * The extra key that contains the Parcelable[] with the new
	 * {@link TimestampedValue}s. Cast the array items one by one, rather than
	 * the entire array, since this will throw a {@link ClassCastException}.
	 */
	public static final String EXTRA_NEW_VALUES = "values";

	/**
	 * The extra key that contains the new {@link TriState}.
	 */
	public static final String EXTRA_NEW_TRISTATE = "tristate";

	/**
	 * The extra key that contains the timestamp of when the evaluation
	 * resulting in the {@link TriState} happened.
	 */
	public static final String EXTRA_NEW_TRISTATE_TIMESTAMP = "timestamp";

	/**
	 * The extra key that can be used with
	 * {@link #registerExpression(Context, String, Expression, Intent, Intent, Intent, Intent)}
	 * intents to indicate what type the intents are. Possible values are
	 * {@link #INTENT_TYPE_ACTIVITY}, {@link #INTENT_TYPE_BROADCAST} (default),
	 * {@link #INTENT_TYPE_SERVICE}.
	 */
	public static final String EXTRA_INTENT_TYPE = "intent_type";

	/**
	 * Extra value for key {@link #EXTRA_INTENT_TYPE} that indicates that the
	 * intent should be broadcast
	 */
	public static final String INTENT_TYPE_BROADCAST = "broadcast";

	/**
	 * Extra value for key {@link #EXTRA_INTENT_TYPE} that indicates that the
	 * intent should start an activity
	 */
	public static final String INTENT_TYPE_ACTIVITY = "activity";

	/**
	 * Extra value for key {@link #EXTRA_INTENT_TYPE} that indicates that the
	 * intent should start a service
	 */
	public static final String INTENT_TYPE_SERVICE = "service";

	/**
	 * Map containing all listeners currently in use, mapped by id of the
	 * expression
	 */
	private static Map<String, ExpressionListener> sListeners = new HashMap<String, ExpressionListener>();

	/**
	 * Boolean indicating whether we received a register to intercept broadcasts
	 * and forward them to the respective listeners
	 */
	private static boolean sReceiverRegistered = false;

	/**
	 * Broadcast receiver used in case values have to be forwarded to listeners
	 */
	private static BroadcastReceiver sReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String id = intent.getData().getFragment();
			if (sListeners.containsKey(id)) {
				if (intent.getAction().equals(ACTION_NEW_VALUES)) {
					// do the conversion from Parcelable[] to
					// TimestampedValue[], casting doesn't work
					Parcelable[] parcelables = (Parcelable[]) intent
							.getParcelableArrayExtra(EXTRA_NEW_VALUES);
					TimestampedValue[] timestampedValues = new TimestampedValue[parcelables.length];
					System.arraycopy(parcelables, 0, timestampedValues, 0,
							parcelables.length);
					sListeners.get(id).onNewValues(id, timestampedValues);
				} else if (intent.getAction().equals(ACTION_NEW_TRISTATE)) {
					sListeners
							.get(id)
							.onNewState(
									id,
									intent.getLongExtra(
											EXTRA_NEW_TRISTATE_TIMESTAMP, 0),
									TriState.valueOf(intent
											.getStringExtra(EXTRA_NEW_TRISTATE)));
				}

			} else {
				Log.d(TAG, "got spurious broadcast: " + intent.getDataString());
			}
		}
	};

	/**
	 * Returns all the information about the sensors known to SWAN. These are
	 * sensors both from within the SWAN framework and 3rd party sensors.
	 * 
	 * @param context
	 * @return List of information per known sensor.
	 */
	public static List<SensorInfo> getSensors(Context context) {
		List<SensorInfo> result = new ArrayList<SensorInfo>();
		Log.d(TAG, "Starting sensor discovery");
		PackageManager pm = context.getPackageManager();
		Intent queryIntent = new Intent("interdroid.swan.sensor.DISCOVER");
		List<ResolveInfo> discoveredSensors = pm.queryIntentActivities(
				queryIntent, PackageManager.GET_META_DATA);
		Log.d(TAG, "Found " + discoveredSensors.size() + " sensors");
		for (ResolveInfo discoveredSensor : discoveredSensors) {
			try {
				Drawable icon = new BitmapDrawable(
						context.getResources(),
						BitmapFactory.decodeResource(
								pm.getResourcesForApplication(discoveredSensor.activityInfo.packageName),
								discoveredSensor.activityInfo.icon));
				Log.d(TAG, "\t" + discoveredSensor.activityInfo.name);
				result.add(new SensorInfo(new ComponentName(
						discoveredSensor.activityInfo.packageName,
						discoveredSensor.activityInfo.name),
						discoveredSensor.activityInfo.metaData, icon));
			} catch (Exception e) {
				Log.e(TAG, "Error with discovered sensor: " + discoveredSensor,
						e);
			}
		}
		return result;
	}

	/**
	 * Returns the information about a sensor with a specific name or throws a
	 * {@link SwanException} if the sensor is not installed on the device.
	 * 
	 * @param context
	 * @param name
	 *            the entity name of the sensor
	 * @return The sensor information
	 * @throws SwanException
	 */
	public static SensorInfo getSensor(Context context, String name)
			throws SwanException {
		for (SensorInfo sensorInfo : getSensors(context)) {
			if (sensorInfo.getEntity().equals(name)) {
				return sensorInfo;
			}
		}
		throw new SwanException("Sensor '" + name + "' not installed.");
	}

	/**
	 * Registers a {@link TriStateExpression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the user provided unique id of the expression. Should not
	 *            contain {@link Expression#SEPARATOR} or end with any of the
	 *            {@link Expression#RESERVED_SUFFIXES}.
	 * @param expression
	 *            the {@link TriStateExpression} that should be evaluated
	 * @param listener
	 *            a {@link TriStateExpressionListener} that receives the
	 *            evaluation results. If this parameter is null, it is also
	 *            possible to listen for the results using a
	 *            {@link BroadcastReceiver}. Filter on datascheme
	 *            "swan://<your.package.name>#<your.expression.id>" and action
	 *            {@link #ACTION_NEW_TRISTATE}.
	 * @throws SwanException
	 *             if id is null or invalid
	 */
	public static void registerTriStateExpression(final Context context,
			final String id, final TriStateExpression expression,
			final TriStateExpressionListener listener) throws SwanException {
		if (listener == null) {
			registerExpression(context, id, expression, null);
		} else {
			registerExpression(context, id, expression,
					new ExpressionListener() {

						@Override
						public void onNewValues(String id,
								TimestampedValue[] newValues) {
							// ignore, will not happen
						}

						@Override
						public void onNewState(String id, long timestamp,
								TriState newState) {
							listener.onNewState(id, timestamp, newState);
						}
					});
		}
	}

	/**
	 * Registers a {@link ValueExpression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the user provided unique id of the expression. Should not
	 *            contain {@link Expression#SEPARATOR} or end with any of the
	 *            {@link Expression#RESERVED_SUFFIXES}.
	 * @param expression
	 *            the {@link ValueExpression} that should be evaluated
	 * @param listener
	 *            a {@link ValueExpressionListener} that receives the evaluation
	 *            results. If this parameter is null, it is also possible to
	 *            listen for the results using a {@link BroadcastReceiver}.
	 *            Filter on datascheme
	 *            "swan://<your.package.name>#<your.expression.id>" and action
	 *            {@link #ACTION_NEW_VALUES}.
	 * @throws SwanException
	 *             if id is null or invalid
	 */
	public static void registerValueExpression(Context context, String id,
			ValueExpression expression, final ValueExpressionListener listener)
			throws SwanException {
		if (listener == null) {
			registerExpression(context, id, expression, null);
		} else {
			registerExpression(context, id, expression,
					new ExpressionListener() {

						@Override
						public void onNewValues(String id,
								TimestampedValue[] newValues) {
							listener.onNewValues(id, newValues);
						}

						@Override
						public void onNewState(String id, long timestamp,
								TriState newState) {
							// ignore, will not happen
						}
					});
		}
	}

	/**
	 * Registers an {@link Expression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the user provided unique id of the expression. Should not
	 *            contain {@link Expression#SEPARATOR} or end with any of the
	 *            {@link Expression#RESERVED_SUFFIXES}.
	 * @param expression
	 *            the {@link ValueExpression} that should be evaluated
	 * @param listener
	 *            a {@link ValueExpressionListener} that receives the evaluation
	 *            results. If this parameter is null, it is also possible to
	 *            listen for the results using a {@link BroadcastReceiver}.
	 *            Filter on datascheme
	 *            "swan://<your.package.name>#<your.expression.id>" and action
	 *            {@link #ACTION_NEW_VALUES} or {@link #ACTION_NEW_TRISTATE}.
	 * @throws SwanException
	 *             if id is null or invalid
	 */
	public static void registerExpression(Context context, String id,
			Expression expression, ExpressionListener expressionListener)
			throws SwanException {
		if (id == null) {
			throw new SwanException("Invalid id. Null is not allowed as id");
		}
		if (id.contains(Expression.SEPARATOR)) {
			throw new SwanException("Invalid id: '" + id
					+ "' contains reserved separator '" + Expression.SEPARATOR
					+ "'");
		}
		for (String suffix : Expression.RESERVED_SUFFIXES) {
			if (id.endsWith(suffix)) {
				throw new SwanException("Invalid id. Suffix '" + suffix
						+ "' is reserved for internal use.");
			}
		}
		if (sListeners.containsKey(id)) {
			throw new SwanException("Listener already registered for id '" + id
					+ "'");
		} else {
			if (expressionListener != null) {
				if (sListeners.size() == 0) {
					sReceiverRegistered = true;
					registerReceiver(context);
				}
				sListeners.put(id, expressionListener);
			}
		}
		Intent newTriState = new Intent(ACTION_NEW_TRISTATE);
		newTriState.setData(Uri.parse("swan://" + context.getPackageName()
				+ "#" + id));
		Intent newValues = new Intent(ACTION_NEW_VALUES);
		newValues.setData(Uri.parse("swan://" + context.getPackageName() + "#"
				+ id));
		registerExpression(context, id, expression, newTriState, newTriState,
				newTriState, newValues);
	}

	/**
	 * Registers a {@link TriStateExpression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the user provided unique id of the expression. Should not
	 *            contain {@link Expression#SEPARATOR} or end with any of the
	 *            {@link Expression#RESERVED_SUFFIXES}.
	 * @param expression
	 *            the {@link TriStateExpression} that should be evaluated
	 * @param onTrue
	 *            Intent that should be fired when state changes to true. By
	 *            default the Intent is used to send a broadcast. Add
	 *            {@link #EXTRA_INTENT_TYPE} with any of the values
	 *            {@link #INTENT_TYPE_ACTIVITY}, {@link #INTENT_TYPE_SERVICE} to
	 *            have Swan launch an activity or service.
	 * @param onFalse
	 *            Intent that should be fired when state changes to false. By
	 *            default the Intent is used to send a broadcast. Add
	 *            {@link #EXTRA_INTENT_TYPE} with any of the values
	 *            {@link #INTENT_TYPE_ACTIVITY}, {@link #INTENT_TYPE_SERVICE} to
	 *            have Swan launch an activity or service.
	 * @param onUndefined
	 *            Intent that should be fired when state changes to undefined.
	 *            By default the Intent is used to send a broadcast. Add
	 *            {@link #EXTRA_INTENT_TYPE} with any of the values
	 *            {@link #INTENT_TYPE_ACTIVITY}, {@link #INTENT_TYPE_SERVICE} to
	 *            have Swan launch an activity or service.
	 */
	public static void registerTriStateExpression(Context context, String id,
			TriStateExpression expression, Intent onTrue, Intent onFalse,
			Intent onUndefined) {
		registerExpression(context, id, expression, onTrue, onFalse,
				onUndefined, null);
	}

	/**
	 * Registers a {@link ValueExpression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the user provided unique id of the expression. Should not
	 *            contain {@link Expression#SEPARATOR} or end with any of the
	 *            {@link Expression#RESERVED_SUFFIXES}.
	 * @param expression
	 *            the {@link ValueExpression} that should be evaluated
	 * @param onNewValues
	 *            Intent that should be fired when new values are available. Add
	 *            {@link #EXTRA_INTENT_TYPE} with any of the values
	 *            {@link #INTENT_TYPE_ACTIVITY}, {@link #INTENT_TYPE_SERVICE} to
	 *            have Swan launch an activity or service.
	 */
	public static void registerValueExpression(Context context, String id,
			TriStateExpression expression, Intent onNewValues) {
		registerExpression(context, id, expression, null, null, null,
				onNewValues);
	}

	private static void registerExpression(Context context, String id,
			Expression expression, Intent onTrue, Intent onFalse,
			Intent onUndefined, Intent onNewValues) {
		Intent intent = new Intent(ACTION_REGISTER);
		intent.putExtra("expressionId", id);
		intent.putExtra("expression", expression.toParseString());
		intent.putExtra("onTrue", onTrue);
		intent.putExtra("onFalse", onFalse);
		intent.putExtra("onUndefined", onUndefined);
		intent.putExtra("onNewValues", onNewValues);
		context.sendBroadcast(intent);
	}

	/**
	 * Unregisters a previously registered {@link Expression} for evaluation.
	 * 
	 * @param context
	 * @param id
	 *            the id with which the expression was registered.
	 */
	public static void unregisterExpression(Context context, String id) {
		sListeners.remove(id);
		if (sListeners.size() == 0 && sReceiverRegistered) {
			sReceiverRegistered = false;
			unregisterReceiver(context);
		}
		Intent intent = new Intent(ACTION_UNREGISTER);
		intent.putExtra("expressionId", id);
		context.sendBroadcast(intent);

	}

	/**
	 * registers the broadcast receiver to receive values on behalve of
	 * listeners and forward them subsequently.
	 * 
	 * @param context
	 */
	private static void registerReceiver(Context context) {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_NEW_TRISTATE);
		intentFilter.addAction(ACTION_NEW_VALUES);
		intentFilter.addDataScheme("swan");
		intentFilter.addDataAuthority(context.getPackageName(), null);
		context.registerReceiver(sReceiver, intentFilter);
	}

	/**
	 * unregisters the broadcast receiver. This is executed if no listeners are
	 * present anymore.
	 * 
	 * @param context
	 */
	private static void unregisterReceiver(Context context) {
		context.unregisterReceiver(sReceiver);
	}

}
