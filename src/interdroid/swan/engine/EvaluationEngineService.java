package interdroid.swan.engine;

import interdroid.swan.ExpressionManager;
import interdroid.swan.R;
import interdroid.swan.SensorConfigurationException;
import interdroid.swan.SwanException;
import interdroid.swan.crossdevice.Converter;
import interdroid.swan.crossdevice.Pusher;
import interdroid.swan.sensors.SensorInterface;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.Parseable;
import interdroid.swan.swansong.Result;
import interdroid.swan.swansong.TimestampedValue;
import interdroid.swan.swansong.TriState;
import interdroid.swan.swansong.ValueExpression;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.PriorityQueue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class EvaluationEngineService extends Service {

	private static final String TAG = "EvaluationEngine";

	private static final String DATABASE_NAME = "swan";
	private static final String TABLE = "expressions";
	private static final int DATABASE_VERSION = 1;
	private static final int NOTIFICATION_ID = 1;

	public static final String ACTION_REGISTER_REMOTE = "interdroid.swan.register_remote";
	public static final String ACTION_UNREGISTER_REMOTE = "interdroid.swan.unregister_remote";
	public static final String ACTION_NEW_RESULT_REMOTE = "interdroid.swan.new_result_remote";

	public static final String UPDATE_EXPRESSIONS = "interdroid.swan.UPDATE_EXPRESSIONS";

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	PriorityQueue<QueuedExpression> mEvaluationQueue = new PriorityQueue<QueuedExpression>();

	/** The context expressions, mapped by id. */
	private final HashMap<String, QueuedExpression> mRegisteredExpressions = new HashMap<String, QueuedExpression>() {
		/**
		 *
		 */
		private static final long serialVersionUID = -658408645837738007L;

		@Override
		public QueuedExpression remove(final Object id) {
			removeFromDb((String) id);
			return super.remove(id);
		}

		@Override
		public QueuedExpression put(final String key,
				final QueuedExpression value) {
			storeToDb(key, value.getExpression());
			return super.put(key, value);
		}

	};

	Thread mEvaluationThread = new Thread() {
		public void run() {
			while (!interrupted()) {
				QueuedExpression head = mEvaluationQueue.peek();
				if (head == null) {
					Log.d(TAG, "Nothing to evaluate!");
					synchronized (mEvaluationThread) {
						try {
							mEvaluationThread.wait();
						} catch (InterruptedException e) {
							continue;
						}
					}
				} else {
					if (head.getDeferUntil() <= System.currentTimeMillis()) {
						// evaluate now
						try {
							//System.out.println("DeferUntil: " + head.getDeferUntil());
							
							long evaluationDelay;
							if(head.getDeferUntil() != 0) {
								evaluationDelay = System.currentTimeMillis() - head.getDeferUntil();
							}else{
								evaluationDelay = 0;
							}
							
							long start = System.currentTimeMillis();
							Result result = mEvaluationManager.evaluate(
									head.getId(), head.getExpression(),
									System.currentTimeMillis());
							head.evaluated((System.currentTimeMillis() - start), evaluationDelay);
							
							if (head.update(result)) {
								Log.d(TAG, "Result: " + result);
								sendUpdate(head.getId(), result);
							}
							// re add the expression to the queue
							synchronized (mEvaluationThread) {
								mEvaluationQueue.remove(head);
								mEvaluationQueue.add(head);
							}
						} catch (SwanException e) {
							Log.d(TAG, "Failed to evaluate", e);
						}
					} else {
						synchronized (mEvaluationThread) {
							try {
								long waitTime = Math.max(
										1,
										head.getDeferUntil()
												- System.currentTimeMillis());
								Log.d(TAG, "Waiting for " + waitTime + " ms.");
								mEvaluationThread.wait(waitTime);
								Log.d(TAG, "Done waiting for " + waitTime
										+ " ms.");
							} catch (InterruptedException e) {
								continue;
							}
						}
					}
				}
			}
		}
	};

	NotificationManager mNotificationManager;
	Notification mNotification;
	EvaluationManager mEvaluationManager;
	
	

	/**
	 * @return all expressions saved in the database.
	 */
	private void restoreAfterBoot() {
		SQLiteDatabase db = openDb();

		try {
			Cursor c = db.query(TABLE, new String[] { "expressionId",
					"expression" }, null, null, null, null, null);
			if (c != null) {
				try {
					if (c.getCount() > 0) {
						while (c.moveToNext()) {
							try {
								String expressionId = c.getString(0);
								Expression expression = ExpressionFactory
										.parse(c.getString(1));
								doRegister(expressionId, expression);
							} catch (Exception e) {
								Log.e(TAG, "Error while restoring after boot.",
										e);
							}
						}
					}
				} finally {
					try {
						c.close();
					} catch (Exception e) {
						Log.w(TAG, "Got exception closing cursor.", e);
					}
				}
			}
		} finally {
			closeDb(db);
		}
		if (mRegisteredExpressions.size() == 0) {
			// that means, we tried to restore, but there is nothing active
			Log.d(TAG, "nothing to restore, shutting down...");
			stopSelf();
		}
	}

	/**
	 * Delete's an expression from the database.
	 * 
	 * @param key
	 *            The id for the expression.
	 * @param type
	 *            The type being removed.
	 */
	private void removeFromDb(final String id) {
		SQLiteDatabase db = openDb();
		try {
			db.execSQL("DELETE FROM " + TABLE + " WHERE expressionId = ?",
					new String[] { id });
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
		Log.d(TAG, "Created db dir: " + dbDir);
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbDir,
				DATABASE_NAME), null);
		Log.d(TAG, "Got database version: " + db.getVersion());
		if (db.getVersion() < DATABASE_VERSION) {
			Log.d(TAG, "Creating table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE "
					+ TABLE
					+ " (_id integer primary key autoincrement, expressionId string, expression string)");
			db.setVersion(DATABASE_VERSION);
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
	private void storeToDb(final String id, final Parseable<?> expression) {
		SQLiteDatabase db = openDb();
		try {
			// Make sure it doesn't exist first in case we are reloading it.
			db.delete(TABLE, "expressionId=?", new String[] { id });
			ContentValues values = new ContentValues();
			values.put("expressionId", id);
			values.put("expression", expression.toParseString());
			db.insert(TABLE, "expressionId", values);
		} finally {
			closeDb(db);
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// we can get several actions here, both from the API and from the
		// Sensors as well as from the Boot event
		String action = intent.getAction();
		if (ExpressionManager.ACTION_REGISTER.equals(action)) {
			String id = intent.getStringExtra("expressionId");
			try {
				Expression expression = ExpressionFactory.parse(intent
						.getStringExtra("expression"));
				
				doRegister(id, expression);
			} catch (Throwable t) {
				Log.d(TAG,
						"Failed to register expression: "
								+ intent.getStringExtra("expression"), t);
			}
		} else if (ExpressionManager.ACTION_UNREGISTER.equals(action)) {
			String id = intent.getStringExtra("expressionId");
			doUnregister(id);
		} else if (ACTION_REGISTER_REMOTE.equals(action)) {
			Log.d(TAG, "Got remote registration");
			Bundle extras = intent.getExtras();
			String regId = extras.getString("source");
			String expId = extras.getString("id");
			String expressionString = extras.getString("data");
			try {
				Expression expression = ExpressionFactory
						.parse(expressionString);
				doRegister(regId + Expression.SEPARATOR + expId, expression);
			} catch (Throwable t) {
				Log.d(TAG, "Failed to register remote expression: "
						+ expressionString, t);
			}
		} else if (ACTION_UNREGISTER_REMOTE.equals(action)) {
			Bundle extras = intent.getExtras();
			String regId = extras.getString("source");
			String expId = extras.getString("id");
			doUnregister(regId + Expression.SEPARATOR + expId);
		} else if (ACTION_NEW_RESULT_REMOTE.equals(action)) {
			Bundle extras = intent.getExtras();
			String id = extras.getString("id");
			Result result = null;
			try {
				result = (Result) Converter.stringToObject(extras
						.getString("data"));
			} catch (Exception e) {
				// should not happen
				throw new RuntimeException("Should not happen. Please debug!");
			}
			mEvaluationManager.newRemoteResult(id, result);
			doNotify(new String[] { id });
			return START_STICKY;
		} else if (SensorInterface.ACTION_NOTIFY.equals(action)) {
			String[] ids = intent.getStringArrayExtra("expressionIds");
			doNotify(ids);
			return START_STICKY;
		} else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			restoreAfterBoot();
		} else if (UPDATE_EXPRESSIONS.equals(action)) {
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					getRegisteredExpressions());
			return START_STICKY;
		}
		// after we handled the intent, we should update the notification and
		// the mode (foreground/background)
		if (mRegisteredExpressions.size() > 0) {
			updateNotification();
			startForeground(NOTIFICATION_ID, mNotification);
		} else {
			stopForeground(true);
		}
		return START_STICKY;
	}

	private void doRegister(final String id, final Expression expression) {
		// handle registration
		if (mRegisteredExpressions.containsKey(id)) {
			// FAIL!
		}
		try {
			mEvaluationManager.initialize(id, expression);
		} catch (SensorConfigurationException e) {
			// FAIL!
			e.printStackTrace();
		} catch (SensorSetupFailedException e) {
			// FAIL!
			e.printStackTrace();
		}
		synchronized (mEvaluationThread) {
			// add this expression to our registered expression, the queue and
			// notify the evaluation thread
			QueuedExpression queued = new QueuedExpression(id, expression);
			mRegisteredExpressions.put(id, queued);
			mEvaluationQueue.add(queued);
			mEvaluationThread.notify();
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					getRegisteredExpressions());
		}

	}

	private Intent getRegisteredExpressions() {
		Intent intent = new Intent(UPDATE_EXPRESSIONS);
		String[] expressions = new String[mRegisteredExpressions.size()];
		int i = 0;
		for (String key : mRegisteredExpressions.keySet()) {
			expressions[i] = mRegisteredExpressions.get(key).toString();
			i++;
		}
		intent.putExtra("expressions", expressions);
		return intent;
	}

	private void doUnregister(final String id) {
		QueuedExpression expression = mRegisteredExpressions.get(id);
		if (expression == null) {
			// FAIL!
		}
		// first stop evaluating
		synchronized (mEvaluationThread) {
			mRegisteredExpressions.remove(id);
			mEvaluationQueue.remove(expression);
			// do we really need to notify the evaluation thread here?
			mEvaluationThread.notify();
			LocalBroadcastManager.getInstance(this).sendBroadcast(
					getRegisteredExpressions());
		}
		// then stop sensing
		mEvaluationManager.stop(id, expression.getExpression());
	}

	// what we get back here are leaf ids of expressions.
	private void doNotify(String[] ids) {
		if (ids == null) {
			return;
		}
		for (String id : ids) {
			String rootId = getRootId(id);
			QueuedExpression queued = mRegisteredExpressions.get(rootId);
			if (queued == null) {
				// TODO: maybe broadcast a message to inform sensors to stop
				// producing values for the id
				Log.d(TAG, "Got notify, but no expression registered with id: "
						+ rootId + " (original id: " + id
						+ "), should we kill the sensor?");
				continue;
			}
			Log.d(TAG, "Got notification for: " + queued);
			if (queued.getExpression() instanceof ValueExpression
					|| queued.getDeferUntil() == Long.MAX_VALUE) {
				// evaluate now!
				synchronized (mEvaluationThread) {
					// get it out the queue, update defer until, and put it
					// back, then notify the evaluation thread.
					mEvaluationQueue.remove(queued);
					queued.update(null);
					mEvaluationQueue.add(queued);
					mEvaluationThread.notifyAll();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public final void onCreate() {
		super.onCreate();
		// construct the sensor manager
		mEvaluationManager = new EvaluationManager(this);
		// kick off the evaluation thread
		mEvaluationThread.start();
		// init the notification stuff
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotification = new Notification(R.drawable.ic_stat_swan,
				"Swan active", System.currentTimeMillis());
		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		mNotification.flags |= Notification.FLAG_NO_CLEAR;
	}

	@Override
	public void onDestroy() {
		mEvaluationManager.destroyAll();
		mEvaluationThread.interrupt();
		super.onDestroy();
	}

	/**
	 * Update notification.
	 */
	private void updateNotification() {
		Intent notificationIntent = new Intent(this,
				ExpressionViewerActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		mNotification.setLatestEventInfo(this, "Swan",
				"number of expressions: " + mRegisteredExpressions.size(),
				contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	private void sendUpdate(String id, Result result) {
		if (id.contains(Expression.SEPARATOR)) {
			// send update to remote!!
			String[] components = id.split(Expression.SEPARATOR, 2);
			sendUpdateToRemote(components[0], components[1], result);
		} else if (result.getTriState() != null) {
			sendTriState(id, result.getTimestamp(), result.getTriState());
		} else if (result.getValues() != null) {
			sendValues(id, result.getValues());
		} else {
			Log.d(TAG, "Send empty update. This should not occur!");
		}
	}

	private void sendUpdateToRemote(final String registrationId,
			final String expressionId, final Result result) {
		// pusher is async
		try {
			Pusher.push(registrationId, expressionId, ACTION_NEW_RESULT_REMOTE,
					Converter.objectToString(result));
		} catch (IOException e) {
			Log.d(TAG, "Exception in converting result to string", e);
		}
	}

	private void sendValues(String id, TimestampedValue[] values) {
		if (values == null) {
			Log.d(TAG, "no update, we have no values...");
			return;
		}
		Intent intent = new Intent(ExpressionManager.ACTION_NEW_VALUES);
		intent.setData(Uri.parse("swanexpression://" + id));
		intent.putExtra(ExpressionManager.EXTRA_NEW_VALUES, values);
		sendBroadcast(intent);
	}

	private void sendTriState(String id, long timestamp, TriState state) {
		Intent intent = new Intent(ExpressionManager.ACTION_NEW_TRISTATE);
		intent.setData(Uri.parse("swanexpression://" + id));
		intent.putExtra(ExpressionManager.EXTRA_NEW_TRISTATE, state.name());
		intent.putExtra(ExpressionManager.EXTRA_NEW_TRISTATE_TIMESTAMP,
				timestamp);
		sendBroadcast(intent);
	}

	// helper function to strip the suffixes for an expression generated by the
	// evaluation engine and retrieve the original user id (the root id)
	private String getRootId(String id) {
		for (String suffix : Expression.RESERVED_SUFFIXES) {
			if (id.endsWith(suffix)) {
				return getRootId(id.substring(0, id.length() - suffix.length()));
			}
		}
		return id;
	}

}
