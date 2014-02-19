package interdroid.swan.crossdevice;

import interdroid.swan.swansong.Expression;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;

public class Registry {

	private static final String TAG = "Registry";
	private static final String TABLE = "SWANRegistry";
	private static final String DATABASE_NAME = "SWANRegistry";
	private static final int DATABASE_VERSION = 1;
	private static boolean sFirstTime = true;
	private static List<String> names = new ArrayList<String>();
	private static Map<String, String> regIds = new HashMap<String, String>();

	private static void setDirty(Context context, boolean dirty) {
		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putBoolean("dirty", dirty).commit();
		sFirstTime = false;
	}

	private static boolean getDirty(Context context) {
		if (sFirstTime) {
			setDirty(context, true);
			sFirstTime = false;
		}
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean("dirty", true);
	}

	public static boolean add(Context context, String name, String regId) {
		if (getNames(context).contains(name)) {
			return false;
		}
		setDirty(context, true);
		storeToDb(context, name, regId);
		return true;
	}

	public static void remove(Context context, String name) {
		removeFromDb(context, name);
		setDirty(context, true);
	}

	public static String get(Context context, String name) {
		if (name.startsWith(Expression.REGID_PREFIX)) {
			return name.substring(Expression.REGID_PREFIX.length() - 1);
		}
		if (getDirty(context)) {
			update(context);
		}
		return regIds.get(name);
	}

	private static void update(Context context) {
		regIds.clear();
		names.clear();
		SQLiteDatabase db = openDb(context);
		try {
			Cursor cursor = db.query(TABLE, new String[] { "name", "regid" },
					null, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				do {
					regIds.put(cursor.getString(0), cursor.getString(1));
					names.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}
		} finally {
			setDirty(context, false);
			closeDb(db);
		}
		// don't show self in the names
		names.remove(Expression.LOCATION_SELF);
	}

	public static List<String> getNames(Context context) {
		if (getDirty(context)) {
			update(context);
		}
		return names;
	}

	private static void closeDb(final SQLiteDatabase db) {
		if (db != null) {
			db.close();
		}
	}

	/**
	 * @return an open database for expressions.
	 */
	private static synchronized SQLiteDatabase openDb(Context context) {
		File dbDir = context.getDir("databases", Context.MODE_PRIVATE);
		Log.d(TAG, "Created db dir: " + dbDir);
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(new File(dbDir,
				DATABASE_NAME), null);
		Log.d(TAG, "Got database version: " + db.getVersion());
		if (db.getVersion() < DATABASE_VERSION) {
			Log.d(TAG, "Creating table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE "
					+ TABLE
					+ " (_id integer primary key autoincrement, name string, regid string)");
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
	private static void storeToDb(final Context context, final String name,
			final String regId) {
		SQLiteDatabase db = openDb(context);
		try {
			// Make sure it doesn't exist first in case we are reloading it.
			db.delete(TABLE, "name=?", new String[] { name });
			ContentValues values = new ContentValues();
			values.put("name", name);
			values.put("regid", regId);
			db.insert(TABLE, "name", values);
		} finally {
			closeDb(db);
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
	private static void removeFromDb(final Context context, final String name) {
		SQLiteDatabase db = openDb(context);
		try {
			db.execSQL("DELETE FROM " + TABLE + " WHERE name = ?",
					new String[] { name });
		} finally {
			closeDb(db);
		}
	}

	public static class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Intent result = new Intent("interdroid.swan.NAMES");
			result.putStringArrayListExtra("names",
					(ArrayList<String>) getNames(context));
			context.sendBroadcast(result);
		}
	}

}
