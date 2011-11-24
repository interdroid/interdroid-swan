package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.ContextServiceConnector;
import interdroid.contextdroid.R;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.Expression;
import interdroid.contextdroid.contextexpressions.Parseable;
import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.contextdroid.ui.LaunchService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import interdroid.contextdroid.test.TestActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The Class ContextService.
 */
public class ContextService extends Service {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ContextService.class);

	/** Identifier for the notification. */
	private static final int SERVICE_NOTIFICATION_ID = 1;

	/**
	 * The name of our expression database.
	 */
	private static final String DATABASE_NAME = "expressions.db";

	/**
	 * The current version of the database.
	 */
	private static final int DB_VERSION = 3;

	/**
	 * The expression type. Used for table and column names.
	 */
	private static final String EXPRESSION_TYPE = "expressions";
	/**
	 * The value type. Used for table and column names.
	 */
	private static final String VALUE_TYPE = "contextvalues";
	/**
	 * The table and column names for types we persist.
	 */
	private static final String[] DB_TYPES = { EXPRESSION_TYPE, VALUE_TYPE };

	/** Are we currently running as a "foreground" service? */
	private boolean mForeground;

	/** The sensor manager. */
	private SensorManager sensorManager;

	/** The notification manager. */
	private NotificationManager mNotificationManager;

	/** The notification. */
	private Notification notification;

	/** The context expressions, mapped by id. */
	private final HashMap<String, Expression> contextExpressions = new HashMap<String, Expression>() {
		/**
		 *
		 */
		private static final long serialVersionUID = -658408645837738007L;

		@Override
		public Expression remove(final Object key) {
			removeFromDb((String) key, EXPRESSION_TYPE);
			return super.remove(key);
		}

		@Override
		public Expression put(final String key, final Expression value) {
			storeToDb(key, value, EXPRESSION_TYPE);
			return super.put(key, value);
		}
	};

	/** The context entities, mapped by id. */
	private final HashMap<String, ContextTypedValue> contextTypedValues = new HashMap<String, ContextTypedValue>() {
		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public ContextTypedValue remove(final Object key) {
			removeFromDb((String) key, VALUE_TYPE);
			return super.remove(key);
		}

		@Override
		public ContextTypedValue put(final String key,
				final ContextTypedValue value) {
			storeToDb(key, value, VALUE_TYPE);
			return super.put(key, value);
		}
	};

	/**
	 * Handles boot notifications so we can reregister expressions.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class BootHandler extends BroadcastReceiver {

		@Override
		public final void onReceive(final Context context, final Intent intent) {
			LOG.debug("Got boot notification!");
			context.startService(new Intent(
					ContextServiceConnector.CONTEXT_SERVICE));
			LOG.debug("Finished handling boot.");
		}

	}

	/** The evaluation queue. */
	private final PriorityQueue<Expression> evaluationQueue = new PriorityQueue<Expression>();

	/** The evaluation queue. */
	private final Queue<ContextTypedValue> contextTypedValueQueue = new LinkedBlockingQueue<ContextTypedValue>();

	/**
	 * The waiting map consists of expressions that resulted in UNDEFINED, they
	 * will remain in this map until the dataset of the sensor causing the
	 * UNDEFINED has a changed data set.
	 */
	private final Map<String, Expression> waitingMap = new HashMap<String, Expression>();

	/**
	 * True if our threads should stop.
	 */
	private boolean shouldStop = false;

	/**
	 * True if we have restored saved expressions.
	 */
	private boolean restoredRegistrations = false;

	/**
	 * Thread responsible for evaluating expressions.
	 */
	private final Thread evaluationThread = new Thread() {
		@Override
		public void run() {
			boolean changed;
			while (!shouldStop) {
				Expression expression = evaluationQueue.peek();
				if (expression == null) {
					synchronized (evaluationQueue) {
						try {
							evaluationQueue.wait();
						} catch (InterruptedException e) {
							LOG.debug("Interrupted while waiting on queue.");
							interrupted();
							continue;
						}

					}
				} else {
					synchronized (evaluationQueue) {
						long waitingTime = expression.getNextEvaluationTime()
								- System.currentTimeMillis();
						if (waitingTime > 0) {
							try {
								LOG.debug("Interrupted while waiting on queue.");
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
						LOG.debug("Queue item already removed.");
						// ignore, it's already out of the queue
					}
					e.printStackTrace();
					LOG.info("{} got deleted", expression);
					continue;
				}
				if (changed) {
					LOG.info("{} has new value: {}", expression,
							expression.getResult());
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

	/**
	 * Thread responsible for managing entities in our expressions.
	 */
	private final Thread entityThread = new Thread() {
		@Override
		public void run() {
			TimestampedValue[] values;
			while (!shouldStop) {
				ContextTypedValue value = contextTypedValueQueue.poll();
				if (value == null) {
					synchronized (contextTypedValueQueue) {
						try {
							contextTypedValueQueue.wait();
						} catch (InterruptedException e) {
							interrupted();
							continue;
						}
					}
				}
				try {
					LOG.debug("Getting values for: {}", value);
					values = value.getValues(value.getId(),
							System.currentTimeMillis());
				} catch (ContextDroidException e) {
					LOG.info("{} unexpected exception ", value, e);
					// TODO: should we send an exception?
					continue;
				} catch (NullPointerException e) {
					// can happen, it means the expression got deleted halfway
					// the evaluation. continue
					LOG.info("{} unexpected exception d", value, e);
					continue;
				}
				sendValuesBroadcastIntent(value.getId(), values);
			}
		}
	};

	@Override
	public final IBinder onBind(final Intent intent) {
		LOG.debug("onBind {}", mBinder);
		return mBinder;
	}

	// =-=-=-=- Expression Database -=-=-=-=

	/**
	 * 
	 * @return all typed values saved in the database.
	 */
	private ContextTypedValue[] getSavedValues() {
		SQLiteDatabase db = openDb();
		ContextTypedValue[] result = null;

		try {
			Cursor c = db.query(VALUE_TYPE, new String[] { "key", VALUE_TYPE },
					null, null, null, null, null);
			ArrayList<ContextTypedValue> values = new ArrayList<ContextTypedValue>();
			if (c != null) {
				try {
					if (c.getCount() > 0) {
						while (c.moveToNext()) {
							try {
								LOG.debug("Parsing: {} {}", c.getString(0),
										c.getString(1));
								ContextTypedValue value = (ContextTypedValue) ContextTypedValue
										.parse(c.getString(1));
								value.setId(c.getString(0));
								values.add(value);
							} catch (Exception e) {
								LOG.error("Error while parsing.", e);
							}
						}
					}
					result = new ContextTypedValue[values.size()];
					values.toArray(result);
				} finally {
					try {
						c.close();
					} catch (Exception e) {
						LOG.warn("Got exception closing cursor.", e);
					}
				}
			}
		} finally {
			closeDb(db);
		}
		return result;
	}

	/**
	 * @return all expressions saved in the database.
	 */
	private Expression[] getSavedExpressions() {
		SQLiteDatabase db = openDb();
		Expression[] result = null;

		try {
			Cursor c = db.query(EXPRESSION_TYPE, new String[] { "key",
					EXPRESSION_TYPE }, null, null, null, null, null);
			ArrayList<Expression> expressions = new ArrayList<Expression>();
			if (c != null) {
				try {
					if (c.getCount() > 0) {
						while (c.moveToNext()) {
							try {
								Expression expression = Expression.parse(c
										.getString(1));
								expression.setId(c.getString(0));
								expressions.add(expression);
							} catch (Exception e) {
								LOG.error("Error while parsing.", e);
							}
						}
					}
					result = new Expression[expressions.size()];
					expressions.toArray(result);
				} finally {
					try {
						c.close();
					} catch (Exception e) {
						LOG.warn("Got exception closing cursor.", e);
					}
				}
			}
		} finally {
			closeDb(db);
		}

		return result;
	}

	/**
	 * Delete's an expression from the database.
	 * 
	 * @param key
	 *            The id for the expression.
	 * @param type
	 *            The type being removed.
	 */
	private void removeFromDb(final String key, final String type) {
		SQLiteDatabase db = openDb();
		try {
			db.execSQL("DELETE FROM " + type + " WHERE key = ?",
					new String[] { key });
		} finally {
			closeDb(db);
		}
	}

	/**
	 * Closes the expression database.
	 */
	private void closeDb(final SQLiteDatabase db) {
		if (db != null) {
			db.close();
		}
	}

	/**
	 * @return an open database for expressions.
	 */
	private synchronized SQLiteDatabase openDb() {
		File dbDir = getDir("databases", Context.MODE_PRIVATE);
		LOG.debug("Created db dir: {}", dbDir);
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbDir,
				DATABASE_NAME), null);
		LOG.debug("Got database: {}", db.getVersion());
		if (db.getVersion() < DB_VERSION) {
			for (String type : DB_TYPES) {
				LOG.debug("Creating table: {}", type);
				db.execSQL("DROP TABLE IF EXISTS " + type);
				db.execSQL("CREATE TABLE " + type + " ("
						+ "_id integer primary key autoincrement,"
						+ "key string," + type + " string)");
			}
			db.setVersion(DB_VERSION);
		}
		return db;
	}

	/**
	 * Stores an expression to the database.
	 * 
	 * @param key
	 *            the key for the expression
	 * @param value
	 *            the expression
	 * @param type
	 *            the type being stored
	 */
	private void storeToDb(final String key, final Parseable<?> value,
			final String type) {
		SQLiteDatabase db = openDb();
		try {
			// Make sure it doesn't exist first in case we are reloading it.
			db.delete(type, "key=?", new String[] { key });
			ContentValues values = new ContentValues();
			values.put("key", key);
			values.put(type, value.toParseString());
			db.insert(type, "key", values);
		} finally {
			closeDb(db);
		}
	}

	/**
	 * Restores all expressions from the database.
	 */
	private void restoreRegistrations() {
		final Expression[] expressions = getSavedExpressions();

		for (Expression expression : expressions) {
			try {
				mBinder.addContextExpression(expression.getId(), expression);
			} catch (RemoteException e) {
				LOG.error("Got exception re-registering expression.", e);
			}
		}

		final ContextTypedValue[] values = getSavedValues();

		for (ContextTypedValue value : values) {
			try {
				mBinder.registerContextTypedValue(value.getId(), value);
			} catch (RemoteException e) {
				LOG.error("Got exception re-registering expression.", e);
			}
		}
	}

	// =-=-=-=- Service Lifecycle -=-=-=-=

	@Override
	public final int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		LOG.debug("onStart: {} {}", intent, flags);

		synchronized (this) {
			if (!restoredRegistrations) {
				restoreRegistrations();
				restoredRegistrations = true;
			}
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public final boolean onUnbind(final Intent intent) {
		LOG.debug("onUnbind");
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
	public final void onCreate() {
		LOG.debug("onCreate");
		super.onCreate();
		sensorManager = new SensorManager(this);
		evaluationThread.start();
		entityThread.start();
		initNotification();
		updateNotification();
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
		manageForegroundState();

		Intent notificationIntent = new Intent(this, TestActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "ContextDroid", "#expressions: "
				+ contextExpressions.size() + ", #entities: "
				+ contextTypedValues.size(), contentIntent);
		mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
	}

	/**
	 * Manage the foreground state of the service.
	 */
	private void manageForegroundState() {
		if (contextExpressions.size() == 0 && contextTypedValues.size() == 0
				&& mForeground) {
			LOG.debug("Setting foreground.");
			this.stopForeground(false);
			mForeground = false;
		} else if ((contextExpressions.size() > 0 || contextTypedValues.size() > 0)
				&& !mForeground) {
			LOG.debug("Setting background.");
			this.startForeground(SERVICE_NOTIFICATION_ID, notification);
			mForeground = true;
		}
	}

	@Override
	public final void onDestroy() {
		LOG.debug("onDestroy");
		super.onDestroy();
		// TODO store and load expressions?
		Intent notificationIntent = new Intent(this, LaunchService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "ContextService",
				"Service stopped", contentIntent);
		notification.flags = 0;
		notification.tickerText = "ContextDroid stopped";
		mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
	}

	/**
	 * Send expression change broadcast intent.
	 * 
	 * @param expression
	 *            the expression
	 */
	protected final void sendExpressionChangeBroadcastIntent(
			final Expression expression) {
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
	 * @param id
	 *            the id of the expression
	 * @param values
	 *            the values for the expression
	 */
	protected final void sendValuesBroadcastIntent(final String id,
			final TimestampedValue[] values) {
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
	 * @param exception
	 *            the exception to send
	 */
	protected final void sendExpressionExceptionBroadcastIntent(
			final Expression expression, final ContextDroidException exception) {
		Intent broadcastIntent = new Intent();
		broadcastIntent.setData(Uri.parse("contextexpression://"
				+ expression.getId()));
		broadcastIntent.setAction(ContextManager.ACTION_EXPRESSIONERROR);
		broadcastIntent.putExtra("exception", exception);
		sendBroadcast(broadcastIntent);
	}

	/** The remote interface. */
	private final IContextService.Stub mBinder = new IContextService.Stub() {

		@Override
		public ContextDroidServiceException addContextExpression(
				final String expressionId, final Expression expression)
				throws RemoteException {
			LOG.debug("Adding context expression: {}. {}", expressionId,
					expression);
			// check whether there already exists an expression with the given
			// identifier, handle appropriately

			if (contextExpressions.containsKey(expressionId)) {
				// for now just throw an exception, may be we should do
				// replacement,
				return new ContextDroidServiceException(
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
				return new ContextDroidServiceException(e);
			}
			contextExpressions.put(expressionId, expression);
			// put the expression at the front of the priorityQueue
			synchronized (evaluationQueue) {
				expression.setDeferUntil(System.currentTimeMillis());
				evaluationQueue.add(expression);
				evaluationQueue.notifyAll();
			}
			updateNotification();
			// no exception so return null
			return null;
		}

		@Override
		public ContextDroidServiceException removeContextExpression(
				final String expressionId) throws RemoteException {
			LOG.debug("Removing expression: {}", expressionId);
			synchronized (evaluationQueue) {
				Expression expression = contextExpressions.remove(expressionId);
				if (!evaluationQueue.remove(expression)) {
					waitingMap.remove(expression.getId());
				}
				evaluationQueue.notifyAll();
				try {
					expression.destroy(expressionId, sensorManager);
				} catch (ContextDroidException e) {
					return new ContextDroidServiceException(e);
				}
			}
			updateNotification();
			// no exception so return null
			return null;
		}

		@Override
		public void shutdown() throws RemoteException {
			for (String expressionId : contextExpressions.keySet()) {
				try {
					contextExpressions.get(expressionId).destroy(expressionId,
							sensorManager);
				} catch (ContextDroidException e) {
					LOG.debug("Got exception while destroying.", e);
					// ignore
				}
			}
			for (String id : contextTypedValues.keySet()) {
				try {
					contextTypedValues.get(id).destroy(id, sensorManager);
				} catch (ContextDroidException e) {
					LOG.debug("Got exception while destroying.", e);
					// ignore
				}
			}
			shouldStop = true;
			evaluationThread.interrupt();
			entityThread.interrupt();
			stopSelf();
		}

		@Override
		public void notifyDataChanged(final String[] ids)
				throws RemoteException {
			for (String id : ids) {
				Expression expression = waitingMap.remove(id);
				if (expression == null) {
					continue;
				}
				expression.setDeferUntil(System.currentTimeMillis());
				synchronized (evaluationQueue) {
					evaluationQueue.add(expression);
					evaluationQueue.notifyAll();
				}
			}
			// TODO look at this below
			for (String id : ids) {
				if (contextTypedValues.containsKey(id)) {
					synchronized (contextTypedValueQueue) {
						contextTypedValueQueue.add(contextTypedValues.get(id));
						contextTypedValueQueue.notifyAll();
					}
				}
				
			}
		}

		@Override
		public ContextDroidServiceException registerContextTypedValue(
				final String id, final ContextTypedValue contextTypedValue)
				throws RemoteException {
			if (contextTypedValues.containsKey(id)) {
				try {
					contextTypedValues.get(id).destroy(id, sensorManager);
				} catch (ContextDroidException e) {
					return new ContextDroidServiceException(e);
				}
			}
			contextTypedValues.put(id, contextTypedValue);
			try {
				contextTypedValue.initialize(id, sensorManager);
			} catch (SensorConfigurationException e) {
				return new ContextDroidServiceException(e);
			} catch (SensorSetupFailedException e) {
				return new ContextDroidServiceException(e);
			}
			synchronized (contextTypedValueQueue) {
				contextTypedValueQueue.add(contextTypedValue);
				contextTypedValueQueue.notifyAll();
			}
			updateNotification();
			// no exception so return null
			return null;
		}

		@Override
		public ContextDroidServiceException unregisterContextTypedValue(
				final String id) throws RemoteException {
			synchronized (contextTypedValueQueue) {
				ContextTypedValue value = contextTypedValues.remove(id);
				if (value != null) {
					contextTypedValueQueue.remove(value);
					contextTypedValueQueue.notifyAll();
					try {
						value.destroy(id, sensorManager);
					} catch (ContextDroidException e) {
						return new ContextDroidServiceException(e);
					}
				}
			}
			updateNotification();
			// no exception so return null
			return null;

		}

	};

}
