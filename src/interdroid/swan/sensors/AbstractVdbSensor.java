package interdroid.swan.sensors;

import interdroid.swan.swansong.TimestampedValue;
import interdroid.vdb.content.EntityUriBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Base class for sensors which store their data into a VDB database.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public abstract class AbstractVdbSensor extends AbstractSensorBase {

	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(AbstractVdbSensor.class);

	/**
	 * Field which represents the timestamp for the reading.
	 */
	private static final String TIMESTAMP_FIELD = "_timestamp";

	/**
	 * The schema for the timestamp fields required on all rows.
	 */
	protected static final String SCHEMA_TIMESTAMP_FIELDS;

	/**
	 * The schema for the timestamp fields required on all rows.
	 */
	protected static final String SCHEMA_ID_FIELDS;

	private static final String EXPRESSION_ID = "_expression_id";

	// Initialize the timestamp fields.
	static {
		// Bug in compiler. Set is called before .replace() !?!?
		String temp = "\n{'name':'" + TIMESTAMP_FIELD + "', "
				+ "'ui.label':'timestamp', " + "'ui.list':'true', "
				+ "'ui.widget':'timestamp', " + "'type':'long'},";

		SCHEMA_TIMESTAMP_FIELDS = temp.replace('\'', '"');

		// Bug in compiler. Set is called before .replace() !?!?
		temp = "\n{'name':'" + EXPRESSION_ID + "', "
				+ "'ui.label':'expression id', " + "'ui.list':'false', "
				+ "'type':'string'},";

		SCHEMA_ID_FIELDS = temp.replace('\'', '"');
	}

	/**
	 * The uri for the database.
	 */
	private Uri uri;

	/**
	 * The schema for the sensor.
	 */
	private Schema schema;

	public abstract String getScheme();

	/**
	 * Initialize this sensor.
	 */
	@Override
	public final void init() {
		schema = Schema.parse(getScheme());
		uri = EntityUriBuilder.nativeUri(schema.getNamespace(),
				schema.getName());
		LOG.debug("Sensor storing to URI: {}", uri);
	}

	// / =-=-=-=- VDB Specific helper methods -=-=-=-=

	/**
	 * Stores the values to the content provider using this service as the
	 * context. Fills in the timestamp and expiration before storing.
	 * 
	 * @param id
	 *            the id
	 * @param values
	 *            the values to store
	 * @param now
	 *            the timestamp
	 */
	public final void putValues(final String id, final ContentValues values,
			final long now) {
		notifyDataChangedForId(id);
		values.put(EXPRESSION_ID, id);
		putValues(getContentResolver(), uri, values, now);
	}

	/**
	 * Stores the values to the content provider using this service as the
	 * context. Fills in the timestamp and expiration before storing.
	 * 
	 * @param values
	 *            the values to store
	 * @param now
	 *            the timestamp
	 */
	public final void putValues(final ContentValues values, final long now) {
		putValues(getContentResolver(), uri, values, now);
		for (Entry<String, Object> key : values.valueSet()) {
			notifyDataChanged(key.getKey());
		}
	}

	/**
	 * Stores the values to the content provider using this given content
	 * resolver. Fills in the timestamp and expiration before storing.
	 * 
	 * @param resolver
	 *            the resolver to store with
	 * @param values
	 *            the values to store
	 * @param now
	 *            the timestamp
	 * @param uri
	 *            the uri to store into
	 */
	public static final void putValues(final ContentResolver resolver,
			final Uri uri, final ContentValues values, final long now) {
		values.put(TIMESTAMP_FIELD, now);
		resolver.insert(uri, values);
	}

	/**
	 * @param fieldName
	 *            the name of the field
	 * @return the type of the field
	 */
	private Type getType(final String fieldName) {
		return schema.getField(fieldName).schema().getType();
	}

	@Override
	public List<TimestampedValue> getValues(final String id, final long now,
			final long timespan) {
		String fieldName = registeredValuePaths.get(id);
		return getValuesForValuePath(fieldName, id, now, timespan);
	}

	public List<TimestampedValue> getValuesForValuePath(final String fieldName,
			String id, final long now, final long timespan) {
		Type fieldType = getType(fieldName);
		Cursor values;
		if (schema.getField(EXPRESSION_ID) != null) {
			values = getValuesCursor(this, uri, new String[] { fieldName },
					now, timespan, id);
		} else {
			values = getValuesCursor(this, uri, new String[] { fieldName },
					now, timespan, null);
		}
		List<TimestampedValue> ret = null;
		if (values != null && values.moveToFirst()) {
			int column = values.getColumnIndex(fieldName);
			ret = new ArrayList<TimestampedValue>(values.getCount());
			do {
				switch (fieldType) {
				case INT:
					ret.add(new TimestampedValue(values.getInt(column), values
							.getLong(0)));
					break;
				case LONG:
					ret.add(new TimestampedValue(values.getLong(column), values
							.getLong(0)));
					break;
				case ENUM:
				case STRING:
					ret.add(new TimestampedValue(values.getString(column),
							values.getLong(0)));
					break;
				case FLOAT:
					ret.add(new TimestampedValue(values.getFloat(column),
							values.getLong(0)));
					break;
				case DOUBLE:
					ret.add(new TimestampedValue(values.getDouble(column),
							values.getLong(0)));
					break;
				case FIXED:
				case BYTES:
					ret.add(new TimestampedValue(values.getBlob(column), values
							.getLong(0)));
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

	/**
	 * @param context
	 *            the context to use to resolve
	 * @param uri
	 *            the uri for the data
	 * @param values
	 *            the values to be pulled
	 * @param now
	 *            the time
	 * @param timespan
	 *            the timespan
	 * @param id
	 * @return a cursor with the value data
	 */
	protected static Cursor getValuesCursor(final Context context,
			final Uri uri, final String[] values, final long now,
			final long timespan, String id) {
		LOG.debug("timespan: {} end: {}", timespan, now);

		String[] projection = new String[values.length + 1];
		System.arraycopy(values, 0, projection, 1, values.length);
		projection[0] = TIMESTAMP_FIELD;

		LOG.debug("Projection: {} {}", projection, projection.length);

		String where = null;
		String[] whereArgs = null;

		// Build where args
		if (id != null) {
			where = EXPRESSION_ID + "=?";

			if (timespan <= 0) {
				whereArgs = new String[] { id };
			} else {
				whereArgs = new String[] { id, String.valueOf(now - timespan) };
			}
		} else {
			if (timespan > 0) {
				whereArgs = new String[] { String.valueOf(now - timespan) };
			}
		}

		Cursor c = null;
		if (timespan <= 0) {

			c = context.getContentResolver().query(uri, projection, where,
					whereArgs,
					// If timespan is zero we just pull the last one in time
					TIMESTAMP_FIELD + " DESC");

		} else {

			c = context.getContentResolver().query(
					uri,
					projection,
					(where == null ? "" : where + " AND ") + TIMESTAMP_FIELD
							+ " >= ? ", whereArgs,
					// If timespan is zero we just pull the last one in time
					TIMESTAMP_FIELD + " ASC");

		}

		return c;
	}
}
