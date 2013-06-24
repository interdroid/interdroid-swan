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

	private static Map<String, ExpressionListener> sListeners = new HashMap<String, ExpressionListener>();

	private static BroadcastReceiver sReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String id = intent.getData().getAuthority();
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
		List<ResolveInfo> discoveredSensors = pm.queryIntentServices(
				queryIntent, PackageManager.GET_META_DATA);
		Log.d(TAG, "Found " + discoveredSensors.size() + " sensors");
		for (ResolveInfo discoveredSensor : discoveredSensors) {
			try {
				Drawable icon = new BitmapDrawable(
						BitmapFactory.decodeResource(pm
								.getResourcesForApplication("interdroid.swan"),
								discoveredSensor.getIconResource()));
				Log.d(TAG, "\tDiscovered sensor: "
						+ discoveredSensor.serviceInfo.packageName + " "
						+ discoveredSensor.serviceInfo.name);
				result.add(new SensorInfo(new ComponentName(
						discoveredSensor.serviceInfo.packageName,
						discoveredSensor.serviceInfo.name),
						discoveredSensor.serviceInfo.metaData, icon));
			} catch (Exception e) {
				Log.e(TAG, "Error with discovered sensor: " + discoveredSensor,
						e);
			}
		}
		return result;
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
	 *            evaluation results. It is also possible to listen for the
	 *            results using a {@link BroadcastReceiver}. Filter on
	 *            datascheme "swanexpression" and action
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
	 *            results. It is also possible to listen for the results using a
	 *            {@link BroadcastReceiver}. Filter on datascheme
	 *            "swanexpression" and action {@link #ACTION_NEW_VALUES}.
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
	 *            results. It is also possible to listen for the results using a
	 *            {@link BroadcastReceiver}. Filter on datascheme
	 *            "swanexpression" and action {@link #ACTION_NEW_VALUES}.
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
					registerReceiver(context);
				}
				sListeners.put(id, expressionListener);
			}
		}

		Intent intent = new Intent(ACTION_REGISTER);
		intent.putExtra("expressionId", id);
		intent.putExtra("expression", expression.toParseString());
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
		if (sListeners.size() == 0) {
			unregisterReceiver(context);
		}
		Intent intent = new Intent(ACTION_UNREGISTER);
		intent.putExtra("expressionId", id);
		context.sendBroadcast(intent);

	}

	private static void registerReceiver(Context context) {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_NEW_TRISTATE);
		intentFilter.addAction(ACTION_NEW_VALUES);
		intentFilter.addDataScheme("swanexpression");
		context.registerReceiver(sReceiver, intentFilter);
	}

	private static void unregisterReceiver(Context context) {
		context.unregisterReceiver(sReceiver);
	}

}
