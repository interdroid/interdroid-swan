package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.LaunchService;
import interdroid.contextdroid.R;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.contextdroid.test.TestActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

// TODO: Auto-generated Javadoc
/**
 * The Class ContextService.
 */
public class ContextService extends Service {

	/** Logging tag. */
	public static final String TAG = "ContextService";

	/** Identifier for the notification */
	private static final int SERVICE_NOTIFICATION_ID = 1;

	/** The sensor manager. */
	private SensorManager sensorManager;

	/** The notification manager. */
	private NotificationManager mNotificationManager;

	/** The notification. */
	private Notification notification;

	/** The context expressions, mapped by id */
	private HashMap<String, Expression> contextExpressions = new HashMap<String, Expression>();

	/** The context entities, mapped by id */
	private HashMap<String, ContextTypedValue> contextTypedValues = new HashMap<String, ContextTypedValue>();

	/** The evaluation queue */
	private PriorityQueue<Expression> evaluationQueue = new PriorityQueue<Expression>();

	/** The evaluation queue */
	private PriorityQueue<ContextTypedValue> contextTypedValueQueue = new PriorityQueue<ContextTypedValue>();

	/**
	 * The waiting map consists of expressions that resulted in UNDEFINED, they
	 * will remain in this map until the dataset of the sensor causing the
	 * UNDEFINED has a changed data set
	 */
	private Map<String, Expression> waitingMap = new HashMap<String, Expression>();

	private boolean shouldStop = false;

	private Thread evaluationThread = new Thread() {
		public void run() {
			boolean changed;
			while (!shouldStop) {
				Expression expression = evaluationQueue.peek();
				if (expression == null) {
					synchronized (evaluationQueue) {
						try {
							evaluationQueue.wait();
						} catch (InterruptedException e) {
						}
						interrupted();
						continue;
					}
				} else {
					synchronized (evaluationQueue) {
						long waitingTime = expression.getNextEvaluationTime()
								- System.currentTimeMillis();
						if (waitingTime > 0) {
							try {
								evaluationQueue.wait(waitingTime);
							} catch (InterruptedException e) {
								interrupted();
								continue;
							}
						}
					}
				}
				try {
					changed = expression.evaluate();
				} catch (ContextDroidException e) {
					// something went wrong
					// remove this entry from our list of context expressions
					evaluationQueue.remove();
					contextExpressions.remove(expression.getId());
					// and send exception to the listener (using broadcast)
					e.printStackTrace();
					sendExpressionExceptionBroadcastIntent(expression, e);
					continue;
				} catch (NullPointerException e) {
					// can happen, it means the expression got deleted halfway
					// the evaluation. continue
					try {
						evaluationQueue.remove();
					} catch (NoSuchElementException e2) {
						// ignore, it's already out of the queue
					}
					e.printStackTrace();
					Log.i(TAG, expression + " got deleted");
					continue;
				}
				if (changed) {
					Log.i(TAG,
							expression + " has new value: "
									+ expression.getResult());
					// send new value to the listener (using broadcast)
					sendExpressionChangeBroadcastIntent(expression);
				}
				if (expression.getResult() == ContextManager.UNDEFINED) {
					waitingMap.put(expression.getId(), expression);
					synchronized (evaluationQueue) {
						evaluationQueue.remove();
					}
				} else {
					synchronized (evaluationQueue) {
						if (contextExpressions.containsKey(expression.getId())) {
							evaluationQueue.add(evaluationQueue.remove());
						}
					}

				}
			}
		}
	};

	private Thread entityThread = new Thread() {
		public void run() {
			TimestampedValue[] values;
			while (!shouldStop) {
				ContextTypedValue value = contextTypedValueQueue.peek();
				if (value == null) {
					synchronized (contextTypedValueQueue) {
						try {
							contextTypedValueQueue.wait();
						} catch (InterruptedException e) {
						}
						interrupted();
						continue;
					}
				} else {
					synchronized (contextTypedValueQueue) {
						long waitingTime = value.deferUntil()
								- System.currentTimeMillis();
						if (waitingTime > 0) {
							try {
								contextTypedValueQueue.wait(waitingTime);
							} catch (InterruptedException e) {
								interrupted();
								continue;
							}
						}
					}
				}
				try {
					values = value.getValues(value.getId(),
							System.currentTimeMillis());
				} catch (ContextDroidException e) {
					e.printStackTrace();
					// something went wrong
					// remove this entry from our list of context expressions
					try {
						contextTypedValueQueue.remove();
					} catch (NoSuchElementException e2) {
						// ignore, it's already out of the queue
					}
					// contextTypedValues.remove(value.getId());
					// and send exception to the listener (using broadcast)

					// TODO: should we send an expression?
					// sendExpressionExceptionBroadcastIntent(value, e);
					continue;
				} catch (NullPointerException e) {
					// can happen, it means the expression got deleted halfway
					// the evaluation. continue
					try {
						contextTypedValueQueue.remove();
					} catch (NoSuchElementException e2) {
						// ignore, it's already out of the queue
					}
					e.printStackTrace();
					Log.i(TAG, value + " got deleted");
					continue;
				}
				sendValuesBroadcastIntent(value.getId(), values);
				synchronized (contextTypedValueQueue) {
					if (contextTypedValues.containsKey(value.getId())) {
						contextTypedValueQueue.add(contextTypedValueQueue
								.remove());
					}
				}
			}
		}
	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		System.out.println("onBind");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		System.out.println("onUnbind");
		synchronized (this) {
			if (contextExpressions.size() == 0
					&& contextTypedValues.size() == 0) {
				shouldStop = true;
				evaluationThread.interrupt();
				entityThread.interrupt();
			}
		}
		return super.onUnbind(intent);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		System.out.println("onCreate");
		super.onCreate();
		sensorManager = new SensorManager(this);
		evaluationThread.start();
		entityThread.start();
		initNotification();
	}

	/**
	 * Inits the notification.
	 */
	private void initNotification() {
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notification = new Notification(R.drawable.context_statusbar_icon,
				"ContextDroid active", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;
	}

	/**
	 * Update notification.
	 */
	private void updateNotification() {
		int numExpressions = contextExpressions.size();

		Intent notificationIntent = new Intent(this, TestActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "ContextDroid", "#expressions: "
				+ numExpressions, contentIntent);
		mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() {
		System.out.println("onDestroy");
		super.onDestroy();
		// TODO store and load expressions?
		Intent notificationIntent = new Intent(this, LaunchService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "ContextService",
				"Service stopped", contentIntent);
		notification.flags = 0;
		notification.tickerText = "service stopped";
		mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
	}

	/**
	 * Send expression change broadcast intent.
	 * 
	 * @param expression
	 *            the expression
	 */
	protected void sendExpressionChangeBroadcastIntent(Expression expression) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setData(Uri.parse("contextexpression://"
				+ expression.getId()));
		switch (expression.getResult()) {
		case ContextManager.TRUE:
			broadcastIntent.setAction(ContextManager.ACTION_EXPRESSIONTRUE);
			break;
		case ContextManager.UNDEFINED:
			broadcastIntent
					.setAction(ContextManager.ACTION_EXPRESSIONUNDEFINED);
			break;
		default:
			broadcastIntent.setAction(ContextManager.ACTION_EXPRESSIONFALSE);
		}
		sendBroadcast(broadcastIntent);
	}

	/**
	 * Send expression change broadcast intent.
	 * 
	 * @param expression
	 *            the expression
	 */
	protected void sendValuesBroadcastIntent(String id,
			TimestampedValue[] values) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setData(Uri.parse("contextvalues://" + id));
		broadcastIntent.setAction(ContextManager.ACTION_NEWREADING);
		broadcastIntent.putExtra("values", values);
		sendBroadcast(broadcastIntent);
	}

	/**
	 * Send expression change broadcast intent.
	 * 
	 * @param expression
	 *            the expression
	 */
	protected void sendExpressionExceptionBroadcastIntent(
			Expression expression, ContextDroidException exception) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setData(Uri.parse("contextexpression://"
				+ expression.getId()));
		broadcastIntent.setAction(ContextManager.ACTION_EXPRESSIONERROR);
		broadcastIntent.putExtra("exception", exception);
		sendBroadcast(broadcastIntent);
	}

	/** The remote interface */
	private final IContextService.Stub mBinder = new IContextService.Stub() {

		@Override
		public void addContextExpression(final String expressionId,
				final Expression expression) throws RemoteException {
			// check whether there already exists an expression with the given
			// identifier, handle appropriately

			if (contextExpressions.containsKey(expressionId)) {
				// for now just throw an exception, may be we should do
				// replacement,
				throw new ContextDroidServiceException(
						new ContextDroidException("expression with id '"
								+ expressionId + "' already exists"));
			}
			// check whether all sensors in the expression exist and accept
			// the
			// configuration, initialize will do discovery and binding if
			// needed
			try {
				expression.initialize(expressionId, sensorManager);
			} catch (ContextDroidException e) {
				throw new ContextDroidServiceException(e);
			}
			contextExpressions.put(expressionId, expression);
			// put the expression at the front of the priorityQueue
			synchronized (evaluationQueue) {
				expression.setNextEvaluationTime(System.currentTimeMillis());
				evaluationQueue.add(expression);
				evaluationQueue.notifyAll();
			}
			updateNotification();
		}

		@Override
		public void removeContextExpression(String expressionId)
				throws RemoteException {
			synchronized (evaluationQueue) {
				Expression expression = contextExpressions.remove(expressionId);
				if (!evaluationQueue.remove(expression)) {
					waitingMap.remove(expression.getId());
				}
				evaluationQueue.notifyAll();
				try {
					expression.destroy(expressionId, sensorManager);
				} catch (ContextDroidException e) {
					throw new ContextDroidServiceException(e);
				}
			}
			updateNotification();
		}

		@Override
		public void shutdown() throws RemoteException {
			for (String expressionId : contextExpressions.keySet()) {
				try {
					contextExpressions.get(expressionId).destroy(expressionId,
							sensorManager);
				} catch (ContextDroidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			for (String id : contextTypedValues.keySet()) {
				try {
					contextTypedValues.get(id).destroy(id, sensorManager);
				} catch (ContextDroidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			shouldStop = true;
			evaluationThread.interrupt();
			entityThread.interrupt();
			stopSelf();
		}

		@Override
		public void notifyDataChanged(String[] ids) throws RemoteException {
			for (String id : ids) {
				Expression expression = waitingMap.remove(id);
				if (expression == null) {
					continue;
				}
				expression.setNextEvaluationTime(System.currentTimeMillis());
				synchronized (evaluationQueue) {
					evaluationQueue.add(expression);
					evaluationQueue.notifyAll();
				}
			}
			for (String id : ids) {
				if (contextTypedValues.containsKey(id)) {
					contextTypedValues.get(id).setNextEvaluationTime(
							System.currentTimeMillis());
				}
				synchronized (contextTypedValueQueue) {
					contextTypedValueQueue.add(contextTypedValues.get(id));
					contextTypedValueQueue.notifyAll();
				}
			}
		}

		@Override
		public void registerContextTypedValue(String id,
				ContextTypedValue contextTypedValue) throws RemoteException {
			if (contextTypedValues.containsKey(id)) {
				try {
					contextTypedValues.get(id).destroy(id, sensorManager);
				} catch (ContextDroidException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			contextTypedValues.put(id, contextTypedValue);
			try {
				contextTypedValue.initialize(id, sensorManager);
			} catch (SensorConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SensorInitializationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			synchronized (contextTypedValueQueue) {
				contextTypedValueQueue.add(contextTypedValue);
				contextTypedValueQueue.notifyAll();
			}
		}

		@Override
		public void unregisterContextTypedValue(String id)
				throws RemoteException {
			synchronized (contextTypedValueQueue) {
				ContextTypedValue value = contextTypedValues.remove(id);
				contextTypedValueQueue.remove(value);
				contextTypedValueQueue.notifyAll();
				try {
					value.destroy(id, sensorManager);
				} catch (ContextDroidException e) {
					throw new ContextDroidServiceException(e);
				}
			}
		}

	};

}
