package interdroid.swan.engine;

import interdroid.swan.ExpressionManager;
import interdroid.swan.SensorConfigurationException;
import interdroid.swan.SensorInfo;
import interdroid.swan.SwanException;
import interdroid.swan.crossdevice.Pusher;
import interdroid.swan.crossdevice.Registry;
import interdroid.swan.sensors.Sensor;
import interdroid.swan.swansong.BinaryLogicOperator;
import interdroid.swan.swansong.Comparator;
import interdroid.swan.swansong.ComparatorResult;
import interdroid.swan.swansong.ComparisonExpression;
import interdroid.swan.swansong.ConstantValueExpression;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.HistoryReductionMode;
import interdroid.swan.swansong.LogicExpression;
import interdroid.swan.swansong.MathOperator;
import interdroid.swan.swansong.MathValueExpression;
import interdroid.swan.swansong.Result;
import interdroid.swan.swansong.SensorValueExpression;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriState;
import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.UnaryLogicOperator;
import interdroid.swan.swansong.ValueExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class EvaluationManager {

	private static final String TAG = "SensorManager";

	private static final long START_UP_TIME_REMOTE = 60 * 1000;

	/** The sensor information. */
	private List<SensorInfo> mSensorList = new ArrayList<SensorInfo>();

	/** The service connections. */
	private final Map<String, ServiceConnection> mConnections = new HashMap<String, ServiceConnection>();

	/** The sensors proxies */
	private final Map<String, Sensor> mSensors = new HashMap<String, Sensor>();

	/** The context (for launching new services). */
	private final Context mContext;

	private final Map<String, Result> mCachedResults = new HashMap<String, Result>();

	public EvaluationManager(Context context) {
		mContext = context;
	}

	public void newRemoteResult(String id, Result result) {
		mCachedResults.put(id, result);
	}

	public void initialize(String id, Expression expression)
			throws SensorConfigurationException, SensorSetupFailedException {
		// should get the sensors start producing data.
		String location = expression.getLocation();
		if (!location.equals(Expression.LOCATION_SELF)
				&& !location.equals(Expression.LOCATION_INDEPENDENT)) {
			initializeRemote(id, expression);
		} else if (expression instanceof LogicExpression) {
			initialize(id + Expression.LEFT_SUFFIX,
					((LogicExpression) expression).getLeft());
			initialize(id + Expression.RIGHT_SUFFIX,
					((LogicExpression) expression).getRight());
		} else if (expression instanceof ComparisonExpression) {
			initialize(id + Expression.LEFT_SUFFIX,
					((ComparisonExpression) expression).getLeft());
			initialize(id + Expression.RIGHT_SUFFIX,
					((ComparisonExpression) expression).getRight());
		} else if (expression instanceof MathValueExpression) {
			initialize(id + Expression.LEFT_SUFFIX,
					((MathValueExpression) expression).getLeft());
			initialize(id + Expression.RIGHT_SUFFIX,
					((MathValueExpression) expression).getRight());
		} else if (expression instanceof SensorValueExpression) {
			// do the real work here, bind to the sensor.
			bindToSensor(id, (SensorValueExpression) expression, false);
		}
	}

	public void stop(String id, Expression expression) {
		// should get the sensors stop producing data.
		String location = expression.getLocation();
		if (!location.equals(Expression.LOCATION_SELF)
				&& !location.equals(Expression.LOCATION_INDEPENDENT)) {
			stopRemote(id, expression);
		}
		if (expression instanceof LogicExpression) {
			stop(id + Expression.LEFT_SUFFIX,
					((LogicExpression) expression).getLeft());
			stop(id + Expression.RIGHT_SUFFIX,
					((LogicExpression) expression).getRight());
		} else if (expression instanceof ComparisonExpression) {
			stop(id + Expression.LEFT_SUFFIX,
					((ComparisonExpression) expression).getLeft());
			stop(id + Expression.RIGHT_SUFFIX,
					((ComparisonExpression) expression).getRight());
		} else if (expression instanceof MathValueExpression) {
			stop(id + Expression.LEFT_SUFFIX,
					((MathValueExpression) expression).getLeft());
			stop(id + Expression.RIGHT_SUFFIX,
					((MathValueExpression) expression).getRight());
		} else if (expression instanceof SensorValueExpression) {
			// do the real work here, unbind from the sensor.
			unbindFromSensor(id);
		}
	}

	public void destroyAll() {
		for (String id : mSensors.keySet()) {
			unbindFromSensor(id);
		}
	}

	public Result evaluate(String id, Expression expression, long now)
			throws SwanException {
		if (expression == null) {
			throw new RuntimeException("This should not happen! Please debug");
		}
		if (mCachedResults.containsKey(id)) {
			if (mCachedResults.get(id).getDeferUntil() > now) {
				return mCachedResults.get(id);
			}
		}
		Result result = null;
		// if the location is remote, result is null or undefined
		String location = expression.getLocation();
		if (!location.equals(Expression.LOCATION_SELF)
				&& !location.equals(Expression.LOCATION_INDEPENDENT)) {
			if (expression instanceof TriStateExpression) {
				if (mCachedResults.containsKey(id)) {
					return mCachedResults.get(id);
				} else {
					result = new Result(now, TriState.UNDEFINED);
				}
			} else if (expression instanceof ValueExpression) {
				// we don't have anything cached, so send an empty result.
				result = new Result(new TimestampedValue[] {});
			}
			result.setDeferUntil(Long.MAX_VALUE);
		} else if (expression instanceof LogicExpression) {
			result = applyLogic(id, (LogicExpression) expression, now);
		} else if (expression instanceof ComparisonExpression) {
			result = doCompare(id, (ComparisonExpression) expression, now);
		} else if (expression instanceof ConstantValueExpression) {
			result = ((ConstantValueExpression) expression).getResult();
		} else if (expression instanceof MathValueExpression) {
			result = doMath(id, (MathValueExpression) expression, now);
		} else if (expression instanceof SensorValueExpression) {
			result = getFromSensor(id, (SensorValueExpression) expression, now);
		}
		if (result != null && result.getDeferUntil() != Long.MAX_VALUE) {
			mCachedResults.put(id, result);
		}
		return result;
	}

	private void initializeRemote(String id, Expression expression)
			throws SensorSetupFailedException {
		// send a push message with 'register' instead of 'initialize',
		// disadvantage is that we will only later on get exceptions
		String fromRegistrationId = Registry.get(mContext,
				Expression.LOCATION_SELF);
		if (fromRegistrationId == null) {
			throw new SensorSetupFailedException(
					"Device not registered with Google Cloud Messaging, unable to use remote sensors.");
		}
		String toRegistrationId = Registry.get(mContext,
				expression.getLocation());
		if (toRegistrationId == null) {
			throw new SensorSetupFailedException(
					"No registration id known for location: "
							+ expression.getLocation());
		}
		// resolve all remote locations in the expression with respect to the
		// new location.
		Pusher.push(
				fromRegistrationId,
				toRegistrationId,
				id,
				EvaluationEngineService.ACTION_REGISTER_REMOTE,
				expression.toCrossDeviceString(mContext,
						expression.getLocation()));

	}

	private void stopRemote(String id, Expression expression) {
		// send a push message with 'unregister'
		String toRegistrationId = Registry.get(mContext,
				expression.getLocation());
		if (toRegistrationId == null) {
			// this should not happen, kill swan
			throw new RuntimeException(
					"No registration id known for location: "
							+ expression.getLocation());
		}
		// resolve all remote locations in the expression with respect to the
		// new location.
		Pusher.push(
				null,
				toRegistrationId,
				id,
				EvaluationEngineService.ACTION_UNREGISTER_REMOTE,
				expression.toCrossDeviceString(mContext,
						expression.getLocation()));
	}

	private boolean bindToSensor(final String id,
			final SensorValueExpression expression, boolean discover)
			throws SensorConfigurationException, SensorSetupFailedException {
		if (discover) {
			// run discovery
			mSensorList.clear();
			Log.d(TAG, "Starting sensor discovery");
			mSensorList = ExpressionManager.getSensors(mContext);
		}
		for (SensorInfo sensorInfo : mSensorList) {
			// improve logging output here
			if (sensorInfo.getEntity().equals(expression.getEntity())) {
				if (sensorInfo.getValuePaths().contains(
						expression.getValuePath())) {
					if (sensorInfo.acceptsConfiguration(expression
							.getConfiguration())) {
						ServiceConnection conn = new ServiceConnection() {

							@Override
							public void onServiceDisconnected(ComponentName name) {
								// we are disconnected for some reason
								Log.d(TAG, "disconnected for id " + id);
							}

							@Override
							public void onServiceConnected(ComponentName name,
									IBinder service) {
								Sensor sensor = Sensor.Stub
										.asInterface(service);
								mSensors.put(id, sensor);
								try {
									sensor.register(id,
											expression.getValuePath(),
											expression.getConfiguration());
								} catch (RemoteException e) {
									Log.e(TAG, "Registration failed!", e);
								}

							}
						};
						Log.d(TAG,
								"binding to sensor: " + sensorInfo.getIntent());
						mContext.bindService(sensorInfo.getIntent(), conn,
								Context.BIND_AUTO_CREATE);
						mConnections.put(id, conn);
						return true;
					} else {
						Log.d(TAG, "Sensor does not accept configuration '"
								+ expression.getConfiguration() + "'");
					}
				} else {
					Log.d(TAG, "No valuepath found for valuepath '"
							+ expression.getValuePath() + "'");
				}
			}
		}
		Log.d(TAG, "No sensor found for entity '" + expression.getEntity()
				+ "'");

		if (!discover) {
			// try again with discovery
			if (bindToSensor(id, expression, true)) {
				return true;
			}
		}
		// still not found?
		throw new SensorSetupFailedException("Failed to bind to service for: "
				+ expression);
	}

	private void unbindFromSensor(final String id) {
		ServiceConnection conn = mConnections.remove(id);
		Sensor sensor = mSensors.remove(id);
		if (sensor != null) {
			try {
				sensor.unregister(id);
			} catch (RemoteException e) {
				Log.d(TAG, "Failed to unregister for id: " + id
						+ ", this should not happen!", e);
			}
		} else {
			Log.d(TAG, "Cannot unregister for id: " + id
					+ ", sensor is null, this should not happen!");
		}
		if (conn != null) {
			mContext.unbindService(conn);
		} else {
			Log.d(TAG, "Failed to unbind for id: " + id
					+ ", connection is null, this should not happen!");
		}
	}

	private boolean leftFirst(String id, LogicExpression expression, long now) {
		// For a binary logic operation it is important to make a clever
		// decision which of the involved expressions is evaluated first.
		// Depending on the result of this evaluation and the logic operator, it
		// is possible to short circuit EVALUATION or stop SENSING.
		//
		// For instance if the first result is TRUE and the operator is OR,
		// there is no need to evaluate the second expression. This is an
		// EVALUATION optimization, that is, it saves on evaluation time. A good
		// strategy would be to start with the expression that is 'cheapest' to
		// evaluate or has the highest likelihood to cause short circuiting.
		//
		// If we short circuit the current EVALUATION, we compute how long we
		// can defer the next evaluation
		// based on the part that we did evaluate. There is a chance though
		// that by evaluating the other part we find out that we can defer new
		// evaluation even further.
		//
		// For example A OR B, where evaluating A is very cheap and often
		// results in TRUE make A a good choice to start evaluating, because the
		// current evaluation is likely to be cheap and fast. However, it might
		// be the case that B also results in TRUE, but is much more suitable
		// for turning off sensors. B might look whether the maximum over the
		// last hour exceeds a particular limit, while A checks whether the
		// average of the last 10 seconds is above a certain threshold. If we
		// find a recent sensor value that makes B true, we can conclude that B
		// remains true for about an hour, which will make the logic expression
		// true for about an hour. Within this hour no evaluation is needed.

		// TODO improve, take "time" into account, if a sensor has time, we
		// should probably evaluate it first...

		// in case we have a unary operator, it doesn't matter at all.
		if (expression.getOperator() instanceof UnaryLogicOperator) {
			return true;
		}

		float pLeftTrue = 0.8f; // the chance that evaluating the left part
								// results in true
		float pRightTrue = 0.5f; // the chance that evaluating the right part
									// results in true
		float leftEvaluationCost = 100;
		float rightEvaluationCost = 300;
		float leftSenseCost = 200;
		float rightSenseCost = 100;

		float leftFirstCost, rightFirstCost;
		// check which evaluation cost is likely to be cheaper
		switch ((BinaryLogicOperator) expression.getOperator()) {
		case AND:
			leftFirstCost = leftEvaluationCost + pLeftTrue
					* rightEvaluationCost;
			rightFirstCost = rightEvaluationCost + pRightTrue
					* leftEvaluationCost;
			return leftFirstCost <= rightFirstCost;
		case OR:
			leftFirstCost = leftEvaluationCost + (1 - pLeftTrue)
					* rightEvaluationCost;
			rightFirstCost = rightEvaluationCost + (1 - pRightTrue)
					* leftEvaluationCost;
			return leftFirstCost <= rightFirstCost;
		}
		// check which evaluation is likely to cause the other to sleep/defer
		switch ((BinaryLogicOperator) expression.getOperator()) {
		case AND:

			leftFirstCost = leftSenseCost + (1 - pLeftTrue) * rightSenseCost;

			rightFirstCost = rightEvaluationCost + pRightTrue
					* leftEvaluationCost;
			return leftFirstCost <= rightFirstCost;
		case OR:
			leftFirstCost = leftEvaluationCost + (1 - pLeftTrue)
					* rightEvaluationCost;
			rightFirstCost = rightEvaluationCost + (1 - pRightTrue)
					* leftEvaluationCost;
			return leftFirstCost <= rightFirstCost;
		}

		return true;
	}

	private Result applyLogic(String id, LogicExpression expression, long now)
			throws SwanException {
		boolean leftFirst = leftFirst(id, expression, now);

		Expression firstExpression = leftFirst ? expression.getLeft()
				: expression.getRight();
		Expression lastExpression = !leftFirst ? expression.getLeft()
				: expression.getRight();
		String firstSuffix = leftFirst ? Expression.LEFT_SUFFIX
				: Expression.RIGHT_SUFFIX;
		String lastSuffix = !leftFirst ? Expression.LEFT_SUFFIX
				: Expression.RIGHT_SUFFIX;

		Result first = evaluate(id + firstSuffix, firstExpression, now);

		if (shortcut(expression, first)) {
			// apply the sleep and be ready to last
			sleepAndBeReady(id + lastSuffix, lastExpression,
					first.getDeferUntil());
			return first;
		}
		Result last = evaluate(id + lastSuffix, lastExpression, now);

		Result result = new Result(now, expression.getOperator().operate(
				first.getTriState(), last.getTriState()));
		if (last.getTriState() == TriState.TRUE
				&& expression.getOperator().equals(BinaryLogicOperator.OR)) {
			result.setDeferUntil(last.getDeferUntil());
		} else {
			result.setDeferUntil(Math.min(first.getDeferUntil(),
					last.getDeferUntil()));
		}
		return result;
	}

	private Result doCompare(String id, ComparisonExpression expression,
			long now) throws SwanException {
		Result left = evaluate(id + Expression.LEFT_SUFFIX,
				expression.getLeft(), now);
		Result right = evaluate(id + Expression.RIGHT_SUFFIX,
				expression.getRight(), now);
		if (left.getValues().length == 0 || right.getValues().length == 0) {
			Log.d(TAG, "No data for: " + expression);
			Result result = new Result(now, TriState.UNDEFINED);
			result.setDeferUntil(Long.MAX_VALUE);
			return result;
		}

		// in here we should terminate as quickly as possible, but get the
		// highest deferUntil, therefore start from recent to old
		// assume left and right are sorted with most recent one first
		ComparatorResult comparatorResult = new ComparatorResult(now,
				expression.getLeft().getHistoryReductionMode(), expression
						.getRight().getHistoryReductionMode());
		// combination ANY, ANY has a tradeoff. We can terminate evaluation as
		// soon as we find a combination that results in true, BUT if we
		// continue we might find a longer deferUntil
		comparatorResult.startOuterLoop();
		int l = 0, r = 0;
		for (l = 0; l < left.getValues().length; l++) {
			comparatorResult.startInnerLoop();
			for (r = 0; r < right.getValues().length; r++) {
				if (comparatorResult.innerResult(evaluatePair(
						expression.getComparator(),
						left.getValues()[l].getValue(),
						right.getValues()[r].getValue()))) {
					break;
				}
			}
			if (comparatorResult.outerResult()) {
				break;
			}
		}
		// if we don't get a break statement, l and r might be off by one (past
		// the last index)
		l = Math.min(left.getValues().length - 1, l);
		r = Math.min(right.getValues().length - 1, r);

		// find out how long this result will remain valid and defer
		// evaluation to that moment
		long leftRemainsValidUntil = remainsValidUntil(expression.getLeft(),
				left.getValues()[l].getTimestamp(), expression.getComparator(),
				comparatorResult.getTriState(), true);
		long rightRemainsValidUntil = remainsValidUntil(expression.getRight(),
				right.getValues()[r].getTimestamp(),
				expression.getComparator(), comparatorResult.getTriState(),
				false);
		comparatorResult.setDeferUntil(Math.min(leftRemainsValidUntil,
				rightRemainsValidUntil));

		return comparatorResult;
	}

	private Result doMath(String id, MathValueExpression expression, long now)
			throws SwanException {
		Result left = evaluate(id + Expression.LEFT_SUFFIX,
				expression.getLeft(), now);
		Result right = evaluate(id + Expression.RIGHT_SUFFIX,
				expression.getRight(), now);

		if (left.getValues().length == 1 || right.getValues().length == 1) {
			TimestampedValue[] values = new TimestampedValue[left.getValues().length
					* right.getValues().length];
			int index = 0;
			for (int i = 0; i < left.getValues().length; i++) {
				for (int j = 0; j < right.getValues().length; j++) {
					values[index++] = operate(left.getValues()[i],
							expression.getOperator(), right.getValues()[j]);
				}
			}
			Result result = new Result(values);
			result.setDeferUntil(values[0].getTimestamp()
					+ getMinHistoryLength(expression));
			return result;
		} else {
			// TODO: we could relax this statement a bit, and allow for
			// cross-product
			throw new SwanException("Unable to combine two arrays, "
					+ "only one of the operands can be an array: "
					+ expression.getOperator());
		}
	}

	private long getMinHistoryLength(ValueExpression expression) {
		if (expression instanceof ConstantValueExpression) {
			return Long.MAX_VALUE;
		} else if (expression instanceof MathValueExpression) {
			return Math.min(
					getMinHistoryLength(((MathValueExpression) expression)
							.getLeft()),
					getMinHistoryLength(((MathValueExpression) expression)
							.getRight()));
		} else if (expression instanceof SensorValueExpression) {
			return ((SensorValueExpression) expression).getHistoryLength();
		} else {
			// should not happen
			return 0;
		}
	}

	private Result getFromSensor(String id, SensorValueExpression expression,
			long now) {
		if (mSensors.get(id) == null) {
			// put logging here
			Log.d(TAG, "not yet bound for: " + id + ", " + expression);
			Result result = new Result(new TimestampedValue[] {});
			// TODO make this a constant (configurable?)
			result.setDeferUntil(System.currentTimeMillis() + 300);
			return result;
		}
		try {
			List<TimestampedValue> values = mSensors.get(id).getValues(id, now,
					expression.getHistoryLength());
			if (values == null || values.size() == 0) {
				Result result = new Result(new TimestampedValue[] {});
				result.setDeferUntil(now + 1000);
				return result;
			}
			TimestampedValue[] reduced = TimestampedValue.applyMode(values,
					expression.getHistoryReductionMode());

			Result result = new Result(reduced);
			if (expression.getHistoryLength() == 0 || reduced == null
					|| reduced.length == 0) {
				// we cannot defer based on values, new values will be retrieved
				// when they arrive
				result.setDeferUntil(Long.MAX_VALUE);
			} else {
				result.setDeferUntil(reduced[0].getTimestamp()
						+ expression.getHistoryLength());
			}
			return result;
		} catch (RemoteException e) {
			Log.e(TAG,
					"Got remote exception while retrieving values for expression "
							+ expression + " with id " + id, e);
		}
		return null;
	}

	private long remainsValidUntil(ValueExpression expression,
			long determiningValueTimestamp, Comparator comparator,
			TriState triState, boolean left) {
		if (expression instanceof MathValueExpression) {
			// math value is valid as long both of its children are valid
			return Math.min(
					remainsValidUntil(
							((MathValueExpression) expression).getLeft(),
							determiningValueTimestamp, comparator, triState,
							left),
					remainsValidUntil(
							((MathValueExpression) expression).getRight(),
							determiningValueTimestamp, comparator, triState,
							left));
		} else if (expression instanceof ConstantValueExpression) {
			return Long.MAX_VALUE;
		} else if (expression instanceof SensorValueExpression) {
			HistoryReductionMode mode = ((SensorValueExpression) expression)
					.getHistoryReductionMode();
			long deferTime = determiningValueTimestamp
					+ ((SensorValueExpression) expression).getHistoryLength();
			// here we need the big table, see thesis

			// symmetric (left or right doesn't matter)
			if (comparator == Comparator.EQUALS
					|| comparator == Comparator.REGEX_MATCH
					|| comparator == Comparator.STRING_CONTAINS) {
				if (triState == TriState.TRUE) {
					if (mode == HistoryReductionMode.ANY) {
						return deferTime;
					}
				} else if (triState == TriState.FALSE) {
					if (mode == HistoryReductionMode.ALL) {
						return deferTime;
					}
				}
			} else if (comparator == Comparator.NOT_EQUALS) {
				if (triState == TriState.TRUE) {
					if (mode == HistoryReductionMode.ALL) {
						return deferTime;
					}
				} else if (triState == TriState.FALSE) {
					if (mode == HistoryReductionMode.ANY) {
						return deferTime;
					}
				}
			}

			// assymetric (left or right does matter)
			if (left) {
				if (comparator == Comparator.GREATER_THAN
						|| comparator == Comparator.GREATER_THAN_OR_EQUALS) {
					if (triState == TriState.TRUE) {
						if (mode == HistoryReductionMode.MAX
								|| mode == HistoryReductionMode.ANY) {
							return deferTime;
						}
					} else if (triState == TriState.FALSE) {
						if (mode == HistoryReductionMode.MIN
								|| mode == HistoryReductionMode.ALL) {
							return deferTime;
						}
					}
				} else if (comparator == Comparator.LESS_THAN
						|| comparator == Comparator.LESS_THAN_OR_EQUALS) {
					if (triState == TriState.TRUE) {
						if (mode == HistoryReductionMode.MIN
								|| mode == HistoryReductionMode.ANY) {
							return deferTime;
						}
					} else if (triState == TriState.FALSE) {
						if (mode == HistoryReductionMode.MAX
								|| mode == HistoryReductionMode.ALL) {
							return deferTime;
						}
					}
				}
			} else {
				if (comparator == Comparator.GREATER_THAN
						|| comparator == Comparator.GREATER_THAN_OR_EQUALS) {
					if (triState == TriState.TRUE) {
						if (mode == HistoryReductionMode.MIN
								|| mode == HistoryReductionMode.ANY) {
							return deferTime;
						}
					} else if (triState == TriState.FALSE) {
						if (mode == HistoryReductionMode.MAX
								|| mode == HistoryReductionMode.ALL) {
							return deferTime;
						}
					}
				} else if (comparator == Comparator.LESS_THAN
						|| comparator == Comparator.LESS_THAN_OR_EQUALS) {
					if (triState == TriState.TRUE) {
						if (mode == HistoryReductionMode.MAX
								|| mode == HistoryReductionMode.ANY) {
							return deferTime;
						}
					} else if (triState == TriState.FALSE) {
						if (mode == HistoryReductionMode.MIN
								|| mode == HistoryReductionMode.ALL) {
							return deferTime;
						}
					}
				}
			}
		}
		return 0;
	}

	private void sleepAndBeReady(final String id, final Expression expression,
			final long readyTime) {
		if (expression instanceof LogicExpression) {
			sleepAndBeReady(id + Expression.LEFT_SUFFIX,
					((LogicExpression) expression).getLeft(), readyTime);
			sleepAndBeReady(id + Expression.RIGHT_SUFFIX,
					((LogicExpression) expression).getRight(), readyTime);
		} else if (expression instanceof ComparisonExpression) {
			sleepAndBeReady(id + Expression.LEFT_SUFFIX,
					((ComparisonExpression) expression).getLeft(), readyTime);
			sleepAndBeReady(id + Expression.RIGHT_SUFFIX,
					((ComparisonExpression) expression).getRight(), readyTime);
		} else if (expression instanceof MathValueExpression) {
			sleepAndBeReady(id + Expression.LEFT_SUFFIX,
					((MathValueExpression) expression).getLeft(), readyTime);
			sleepAndBeReady(id + Expression.RIGHT_SUFFIX,
					((MathValueExpression) expression).getRight(), readyTime);
		} else if (expression instanceof SensorValueExpression) {
			String location = expression.getLocation();
			// do the real work here, let the sensor stop produce values until
			// ready time.
			long sensorStartUpTime = 0;
			if (!location.equals(Expression.LOCATION_SELF)
					&& !location.equals(Expression.LOCATION_INDEPENDENT)) {
				sensorStartUpTime = START_UP_TIME_REMOTE;
			} else {
				try {
					mSensors.get(id).getStartUpTime(id);
				} catch (RemoteException e) {
					Log.d(TAG,
							"Got unexpected remote exception while retrieving startup time",
							e);
				}
			}
			final long sensorReadyTime = readyTime - sensorStartUpTime;

			if (sensorReadyTime < System.currentTimeMillis()) {
				return;
			}

			unbindFromSensor(id);

			// this should probably be replaced by an android alarm.
			new Thread("sleepAndBeReady-" + id) {
				public void run() {
					try {
						sleep(sensorReadyTime - System.currentTimeMillis());
					} catch (InterruptedException e) {
						// should not happen
					}
					try {
						bindToSensor(id, (SensorValueExpression) expression,
								false);
					} catch (SensorConfigurationException e) {
						Log.d(TAG, "This should not happen!", e);
					} catch (SensorSetupFailedException e) {
						Log.d(TAG,
								"Failed to re bind after sleep and be ready", e);
					}
				}
			}.start();
		}
	}

	private boolean shortcut(LogicExpression expression, Result first) {
		// Can we short circuit and don't evaluate the last expression?
		// FALSE && ?? -> FALSE
		// TRUE || ?? -> TRUE

		// the only drawback of short circuiting is that we could have
		// FALSE && FALSE
		// TRUE || TRUE
		// where the last result has a higher defer until, than the first. But
		// the leftFirst() method should prevent this to happen.
		if (expression.getOperator() instanceof UnaryLogicOperator) {
			// we can always shortcut unary logic operators (there is no last
			// expression).
			return true;
		}
		if ((first.getTriState() == TriState.FALSE && expression.getOperator()
				.equals(BinaryLogicOperator.AND))
				|| ((first.getTriState() == TriState.TRUE) && expression
						.getOperator().equals(BinaryLogicOperator.OR))) {
			return true;
		}
		return false;
	}

	/**
	 * Evaluates a leaf item performing the comparison.
	 * 
	 * @param left
	 *            the left side values
	 * @param right
	 *            the right side values
	 * @return Result.FALSE or Result.TRUE
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private TriState evaluatePair(final Comparator comparator,
			final Object left, final Object right) {
		TriState result = TriState.FALSE;
		switch (comparator) {
		case LESS_THAN:
			if (((Comparable) left).compareTo(right) < 0) {
				result = TriState.TRUE;
			}
			break;
		case LESS_THAN_OR_EQUALS:
			if (((Comparable) left).compareTo(right) <= 0) {
				result = TriState.TRUE;
			}
			break;
		case GREATER_THAN:
			if (((Comparable) left).compareTo(right) > 0) {
				result = TriState.TRUE;
			}
			break;
		case GREATER_THAN_OR_EQUALS:
			if (((Comparable) left).compareTo(right) >= 0) {
				result = TriState.TRUE;
			}
			break;
		case EQUALS:
			if (((Comparable) left).compareTo(right) == 0) {
				result = TriState.TRUE;
			}
			break;
		case NOT_EQUALS:
			if (((Comparable) left).compareTo(right) != 0) {
				result = TriState.TRUE;
			}
			break;
		case REGEX_MATCH:
			if (((String) left).matches((String) right)) {
				result = TriState.TRUE;
			}
			break;
		case STRING_CONTAINS:
			if (((String) left).contains((String) right)) {
				result = TriState.TRUE;
			}
			break;
		default:
			throw new AssertionError("Unknown comparator '" + comparator
					+ "'. Should not happen");
		}
		return result;
	}

	/**
	 * Performs the operation on the requested values.
	 * 
	 * @param left
	 *            the left side
	 * @param right
	 *            the right side
	 * @return the timestamped values
	 * @throws SwanException
	 *             if someting goes wrong
	 */
	private TimestampedValue operate(final TimestampedValue left,
			MathOperator operator, final TimestampedValue right)
			throws SwanException {
		if (left.getValue() instanceof Double
				&& right.getValue() instanceof Double) {
			return new TimestampedValue(operateDouble((Double) left.getValue(),
					operator, (Double) right.getValue()), left.getTimestamp());
		} else if (left.getValue() instanceof Long
				&& right.getValue() instanceof Long) {
			return new TimestampedValue(operateLong((Long) left.getValue(),
					operator, (Long) right.getValue()), left.getTimestamp());
		} else if (left.getValue() instanceof String
				&& right.getValue() instanceof String) {
			return new TimestampedValue(operateString((String) left.getValue(),
					operator, (String) right.getValue()), left.getTimestamp());
		} else if (left.getValue() instanceof Location
				&& right.getValue() instanceof Location) {
			return new TimestampedValue(operateLocation(
					(Location) left.getValue(), operator,
					(Location) right.getValue()), left.getTimestamp());
		}

		throw new SwanException("Trying to operate on incompatible types: "
				+ left.getValue().getClass() + " and "
				+ right.getValue().getClass());
	}

	/**
	 * Operates on doubles.
	 * 
	 * @param left
	 *            the left side value
	 * @param right
	 *            the right side value
	 * @return the combined value
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	private Double operateDouble(final double left, MathOperator operator,
			final double right) throws SwanException {
		Double ret;
		switch (operator) {
		case MINUS:
			ret = left - right;
			break;
		case PLUS:
			ret = left + right;
			break;
		case TIMES:
			ret = left * right;
			break;
		case DIVIDE:
			ret = left / right;
			break;
		case MOD:
			ret = left % right;
		default:
			throw new SwanException("Unknown operator: '" + operator
					+ "' for type Double");
		}
		return ret;
	}

	/**
	 * Operates on longs.
	 * 
	 * @param left
	 *            the left side value
	 * @param right
	 *            the right side value
	 * @return the combined value
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	private Long operateLong(final long left, MathOperator operator,
			final long right) throws SwanException {
		Long ret;
		switch (operator) {
		case MINUS:
			ret = left - right;
			break;
		case PLUS:
			ret = left + right;
			break;
		case TIMES:
			ret = left * right;
			break;
		case DIVIDE:
			ret = left / right;
			break;
		case MOD:
			ret = left % right;
		default:
			throw new SwanException("Unknown operator: '" + operator
					+ "' for type Long");
		}
		return ret;
	}

	/**
	 * Operates on string.
	 * 
	 * @param left
	 *            the left side value
	 * @param right
	 *            the right side value
	 * @return the combined value
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	private String operateString(final String left, MathOperator operator,
			final String right) throws SwanException {
		String ret;
		switch (operator) {
		case PLUS:
			ret = left + right;
			break;
		default:
			throw new SwanException("Unknown operator: '" + operator
					+ "' for type String");
		}
		return ret;
	}

	/**
	 * Operates on locations.
	 * 
	 * @param left
	 *            the left side value
	 * @param right
	 *            the right side value
	 * @return the combined value
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	private Float operateLocation(final Location left, MathOperator operator,
			final Location right) throws SwanException {
		Float ret;
		switch (operator) {
		case MINUS:
			float[] results = new float[3];
			Location.distanceBetween(left.getLatitude(), left.getLongitude(),
					right.getLatitude(), right.getLongitude(), results);
			ret = results[0];
			break;
		default:
			throw new SwanException("Unknown operator: '" + operator
					+ "' for type Location");
		}
		return ret;
	}

}
