package interdroid.contextdroid.sensors;

import java.util.ArrayList;
import java.util.List;

import interdroid.contextdroid.contextexpressions.TimestampedValue;
import interdroid.vdb.content.EntityUriBuilder;

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
	private static final Logger LOG =
			LoggerFactory.getLogger(AbstractVdbSensor.class);

	/**
	 * Field which represents the timestamp for the reading.
	 */
	private static final String	TIMESTAMP_FIELD	= "_timestamp";

	/**
	 * Field which represents the expiration for the reading.
	 */
	private static final String	EXPIRE_FIELD	= "_expiration";

	/**
	 * The schema for the timestamp fields required on all rows.
	 */
	protected static final String	SCHEMA_TIMESTAMP_FIELDS;

	// Initialize the timestamp fields.
	static {
		// Bug in compiler. Set is called before .replace() !?!?
		String temp =
			"\n{'name':'" + TIMESTAMP_FIELD + "', "
					+ "'ui.label':'timestamp', "
					+ "'ui.list':'true', "
					+ "'ui.widget':'timestamp', "
					+ "'type':'long'},"

			+ "\n{'name':'" + EXPIRE_FIELD + "', "
					+ "'ui.label':'expiration', "
					+ "'ui.widget':'timestamp', "
					+ "'type':'long'},";

		SCHEMA_TIMESTAMP_FIELDS = temp.replace('\'', '"');
	}

	/**
	 * The uri for the database.
	 */
	private Uri uri;

	/**
	 * The schema for the sensor.
	 */
	private Schema schema;

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

	/// =-=-=-=- VDB Specific helper methods -=-=-=-=

	/**
	 * Stores the values to the content provider using
	 * this service as the context. Fills in the timestamp
	 * and expiration before storing.
	 * @param values the values to store
	 * @param now the timestamp
	 * @param expire the expiration time
	 */
	public final void putValues(final ContentValues values,
			final long now, final long expire) {
		putValues(getContentResolver(), uri, values, now, expire);
	}

	/**
	 * Stores the values to the content provider using
	 * this given content resolver. Fills in the timestamp
	 * and expiration before storing.
	 * @param resolver the resolver to store with
	 * @param values the values to store
	 * @param now the timestamp
	 * @param uri the uri to store into
	 * @param expire the expiration time
	 */
	public static final void putValues(final ContentResolver resolver,
			final Uri uri, final ContentValues values,
			final long now, final long expire) {
		values.put(TIMESTAMP_FIELD, now);
		values.put(EXPIRE_FIELD, expire);
		resolver.insert(uri, values);
	}

	/**
	 * @param fieldName the name of the field
	 * @return the type of the field
	 */
	private Type getType(final String fieldName) {
		return schema.getField(fieldName).schema().getType();
	}

	@Override
	public final List<TimestampedValue> getValues(final String id,
			final long now, final long timespan) {
		String fieldName = registeredValuePaths.get(id);
		Type fieldType = getType(fieldName);
		Cursor values = getValuesCursor(this, uri,
				new String[] {fieldName}, now, timespan);
		List<TimestampedValue> ret = null;
		if (values != null && values.moveToFirst()) {
			ret = new ArrayList<TimestampedValue>(values.getCount());
			do {
				switch (fieldType) {
				case INT:
					ret.add(new TimestampedValue(values.getInt(2),
							values.getLong(0), values.getLong(1)));
					break;
				case LONG:
					ret.add(new TimestampedValue(values.getLong(2),
							values.getLong(0), values.getLong(1)));
					break;
				case ENUM:
				case STRING:
					ret.add(new TimestampedValue(values.getString(2),
							values.getLong(0), values.getLong(1)));
					break;
				case FLOAT:
					ret.add(new TimestampedValue(values.getFloat(2),
							values.getLong(0), values.getLong(1)));
					break;
				case DOUBLE:
					ret.add(new TimestampedValue(values.getDouble(2),
							values.getLong(0), values.getLong(1)));
					break;
				case FIXED:
				case BYTES:
					ret.add(new TimestampedValue(values.getBlob(2),
							values.getLong(0), values.getLong(1)));
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
	 * @param context the context to use to resolve
	 * @param uri the uri for the data
	 * @param values the values to be pulled
	 * @param now the time
	 * @param timespan the timespan
	 * @return a cursor with the value data
	 */
	public static Cursor getValuesCursor(final Context context,
			final Uri uri,
			final String[] values, final long now,
			final long timespan) {
		LOG.debug("timespan: {} end: {}", timespan, now);

		String[] projection = new String[values.length + 2];
		System.arraycopy(values, 0, projection, 2, values.length);
		projection[0] = TIMESTAMP_FIELD;
		projection[1] = EXPIRE_FIELD;

		LOG.debug("Projection: {}", projection);

		Cursor c = null;
		if (timespan <= 0) {

			c = context.getContentResolver().query(uri,
					projection,
					EXPIRE_FIELD + " >= ?",
					new String[] {String.valueOf(now)},
					// If timespan is zero we just pull the last one in time
					TIMESTAMP_FIELD + " DESC");

		} else {

			c = context.getContentResolver().query(uri,
					projection,
					TIMESTAMP_FIELD + " >= ? AND " + EXPIRE_FIELD + " >= ?",
					new String[] {String.valueOf(now - timespan),
					String.valueOf(now)},
					// If timespan is zero we just pull the last one in time
					TIMESTAMP_FIELD + " ASC");

		}

		return c;
	}

}
