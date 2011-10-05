package interdroid.contextdroid.sensors;

import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.vdb.content.EntityUriBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

// TODO: Auto-generated Javadoc
/**
 * Abstract class that implements basic functionality for sensors. Descendants
 * only have to implement requestReading() and onEntityServiceLevelChange(). The
 * rest can be overridden optionally.
 */
public abstract class AbstractAsynchronousSensor extends Service {

	private static final Logger LOG =
			LoggerFactory.getLogger(AbstractAsynchronousSensor.class);

	private static final String	TIMESTAMP_FIELD	= "_timestamp";

	private static final String	EXPIRE_FIELD	= "_expiration";

	protected static final String	SCHEMA_TIMESTAMP_FIELDS	=
			"\n{'name':'" + TIMESTAMP_FIELD + "', 'type':'long'}," +
			"\n{'name':'" + EXPIRE_FIELD + "', 'type':'long'},"
			.replace('\'', '"');

	private Uri uri;

	private Schema schema;

	protected String[] VALUE_PATHS;

	public final Bundle DEFAULT_CONFIGURATION = new Bundle();

	protected Bundle currentConfiguration = new Bundle();

	private Map<String, List<TimestampedValue>> values = new HashMap<String, List<TimestampedValue>>();

	protected Map<String, Bundle> registeredConfigurations = new HashMap<String, Bundle>();

	protected Map<String, String> registeredValuePaths = new HashMap<String, String>();

	protected Map<String, List<String>> expressionIdsPerValuePath = new HashMap<String, List<String>>();

	/** The context manager. */
	protected SensorContextServiceConnector contextServiceConnector;

	private HashMap<String, Boolean> notified = new HashMap<String, Boolean>();

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onBind(android.content.Intent)
	 *
	 * returns the sensor interface
	 */
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onCreate()
	 *
	 * Creates the ContextManager and connects to the ContextDroid service.
	 */
	@Override
	public void onCreate() {
		contextServiceConnector = new SensorContextServiceConnector(this);
		contextServiceConnector.start();

		VALUE_PATHS = getValuePaths();
		for (String valuePath : VALUE_PATHS) {
			expressionIdsPerValuePath.put(valuePath, new ArrayList<String>());
			values.put(valuePath, Collections
					.synchronizedList(new ArrayList<TimestampedValue>()));
		}

		schema = Schema.parse(getScheme());
		uri = EntityUriBuilder.nativeUri(schema.getNamespace(), schema.getName());
		LOG.debug("Sensor storing to URI: {}", uri);

		initDefaultConfiguration(DEFAULT_CONFIGURATION);
		onConnected();
	}

	public abstract void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION);

	public abstract String[] getValuePaths();

	/**
	 * Callback when connection to ContextDroid has been set up.
	 */
	protected void onConnected() {
		// no actions required
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see android.app.Service#onDestroy()
	 *
	 * Stops the connection to ContextDroid
	 */
	@Override
	public void onDestroy() {
		System.out.println("unbind context service from: " + getClass());
		contextServiceConnector.stop();
		super.onDestroy();
	}

	protected final void notifyDataChangedForId(String id) {
		String rootId = getRootIdFor(id);
		synchronized (this) {
			if (!notified.get(rootId)) {
				notified.put(rootId, true);
			}
		}
		contextServiceConnector.notifyDataChanged(new String[] { rootId });
	}

	private String getRootIdFor(String id) {
		while (id.endsWith(".R") || id.endsWith(".L")) {
			id = id.substring(0, id.length() - 2);
		}
		return id;
	}

	protected final void notifyDataChanged(String valuePath) {
		List<String> notify = new ArrayList<String>();

		synchronized (this) {
			for (String id : expressionIdsPerValuePath.get(valuePath)) {
				id = getRootIdFor(id);
				if (!notified.get(id)) {
					notify.add(id);
					notified.put(id, true);
				}
			}
		}

		if (notify.size() > 0) {
			contextServiceConnector.notifyDataChanged(notify
					.toArray(new String[notify.size()]));
		}
	}

	protected abstract void register(String id, String valuePath,
			Bundle configuration);

	protected abstract void unregister(String id);

	protected abstract String getScheme();

	private void printState() {
		for (String key : notified.keySet()) {
			System.out.println("not: " + key + ": " + notified.get(key));
		}
		for (String key : registeredConfigurations.keySet()) {
			System.out.println("conf: " + key + ": "
					+ registeredConfigurations.get(key));
		}
		for (String key : registeredValuePaths.keySet()) {
			System.out.println("vp: " + key + ": "
					+ registeredValuePaths.get(key));
		}
		for (String key : expressionIdsPerValuePath.keySet()) {
			System.out.println("expressionIds: " + key + ": "
					+ expressionIdsPerValuePath.get(key));
		}

	}

	/** The m binder. */
	private final IAsynchronousContextSensor.Stub mBinder = new IAsynchronousContextSensor.Stub() {

		@Override
		public void register(String id, String valuePath, Bundle configuration)
				throws RemoteException {
			synchronized (AbstractAsynchronousSensor.this) {
				notified.put(getRootIdFor(id), false);
				registeredConfigurations.put(id, configuration);
				registeredValuePaths.put(id, valuePath);
				expressionIdsPerValuePath.get(valuePath).add(id);
				// printState();

				AbstractAsynchronousSensor.this.register(id, valuePath,
						configuration);
			}
		}

		@Override
		public void unregister(String id) throws RemoteException {
			notified.remove(getRootIdFor(id));
			registeredConfigurations.remove(id);
			String valuePath = registeredValuePaths.remove(id);
			expressionIdsPerValuePath.get(valuePath).remove(id);
			// printState();
			AbstractAsynchronousSensor.this.unregister(id);
		}

		@Override
		public List<TimestampedValue> getValues(String id, long now,
				long timespan) throws RemoteException {
			synchronized (AbstractAsynchronousSensor.this) {
				notified.put(getRootIdFor(id), false);
			}
			try {
				return AbstractAsynchronousSensor.this.getValues(id, now,
						timespan);
			} catch (Throwable t) {
				t.printStackTrace();
			}
			return null;
		}

		@Override
		public String getScheme() throws RemoteException {
			return AbstractAsynchronousSensor.this.getScheme();
		}

	};

	/**
	 * Gets all readings from timespan seconds ago until now. Readings are in
	 * reverse order (latest first). This is important for the expression
	 * engine.
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return All readings in the timespan between timespan seconds ago and now
	 */
	public static List<TimestampedValue> getValuesForTimeSpan(
			List<TimestampedValue> values, long now, long timespan) {
		// make a copy of the list
		List<TimestampedValue> result = new ArrayList<TimestampedValue>();
		result.addAll(values);

		if (timespan == 0) {
			// only return latest
			return result
					.subList(Math.max(0, result.size() - 1), result.size());
		}

		int startPos = 0;
		int endPos = 0;
		for (int i = 0; i < values.size(); i++) {
			if ((now - timespan) > values.get(i).timestamp) {
				startPos++;
			}
			if (now > values.get(i).timestamp) {
				endPos++;
			}
		}
		return result.subList(startPos, endPos);

	}

	protected void trimValues(int history) {
		for (String path : VALUE_PATHS) {
			if (values.get(path).size() >= history) {
				values.get(path).remove(0);
			}
		}
	}

	protected void putValue(String valuePath, long now, long expire,
			Object value) {
		values.get(valuePath).add(
				new TimestampedValue(value, now, expire));
		notifyDataChanged(valuePath);
	}

	protected void putValues(final ContentValues values,
			long now, long expire) {
		values.put(TIMESTAMP_FIELD, now);
		values.put(EXPIRE_FIELD, expire);
		getContentResolver().insert(uri, values);
	}

	protected void trimValueByTime(long expire) {
		for (String valuePath : VALUE_PATHS) {
			while ((values.get(valuePath).size() > 0
					&& values.get(valuePath).get(0).timestamp < expire)) {
				values.get(valuePath).remove(0);
			}
		}
	}

	protected List<TimestampedValue> getValues(String id, long now, long timespan) {
		String fieldName = registeredValuePaths.get(id);
		Type fieldType = getType(fieldName);
		Cursor values = getContentResolver().query(uri,
				new String[] {TIMESTAMP_FIELD, EXPIRE_FIELD,
					fieldName},
				TIMESTAMP_FIELD + " > ? AND " + EXPIRE_FIELD + " < ?",
				new String[] {String.valueOf(now - timespan),
					String.valueOf(now)},
				// If timespan is zero we just pull the last one in time
				TIMESTAMP_FIELD + (timespan > 0 ? " ASC" : " DESC"));
		List<TimestampedValue> ret = null;
		if (values != null && values.moveToFirst()) {
			ret = new ArrayList<TimestampedValue>(values.getCount());
			do {
				switch (fieldType) {
				case INT:
					ret.add(new TimestampedValue(values.getInt(2), values.getLong(0), values.getLong(1)));
					break;
				case LONG:
					ret.add(new TimestampedValue(values.getLong(2), values.getLong(0), values.getLong(1)));
					break;
				case ENUM:
				case STRING:
					ret.add(new TimestampedValue(values.getString(2), values.getLong(0), values.getLong(1)));
					break;
				case FLOAT:
					ret.add(new TimestampedValue(values.getFloat(2), values.getLong(0), values.getLong(1)));
					break;
				case DOUBLE:
					ret.add(new TimestampedValue(values.getDouble(2), values.getLong(0), values.getLong(1)));
					break;
				case FIXED:
				case BYTES:
					ret.add(new TimestampedValue(values.getBlob(2), values.getLong(0), values.getLong(1)));
				default:
					throw new RuntimeException("Unsupported type.");
				}
				// Limit to one result if timespan is zero
				if (timespan == 0) {
					break;
				}
			} while (values.moveToNext());
		}
		try {
			if (values != null) {
				values.close();
			}
		} catch (Exception e) {
			LOG.warn("Error closing cursor ignored.", e);
		}
		if (ret == null) {
			ret = new ArrayList<TimestampedValue>(0);
		}
		return ret;
	}

	private Type getType(String fieldName) {
		return schema.getField(fieldName).schema().getType();
	}

	protected List<TimestampedValue> getMemoryValues(String id, long now, long timespan) {
		return getValuesForTimeSpan(values.get(registeredValuePaths.get(id)),
				now, timespan);
	}

}
