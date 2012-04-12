package interdroid.swan.sensors;

import interdroid.swan.ContextManager;

import java.util.Arrays;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * Connector back to the sensor service.
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class SensorContextServiceConnector extends ContextManager {

	/**
	 * Construct a connector back to the sensor service.
	 * @param context the context the connector runs in.
	 */
	public SensorContextServiceConnector(final Context context) {
		super(context);
	}

	/**
	 * Sends notification that data changed for the given IDs.
	 * @param ids the ids to notify about.
	 */
	public final void notifyDataChanged(final String[] ids) {
		try {
			getContextService().notifyDataChanged(ids);
		} catch (RemoteException e) {
			Log.e(getClass().getName(),
					"Unable to notify context service for ids: "
			+ Arrays.toString(ids));
		}
	}

}
