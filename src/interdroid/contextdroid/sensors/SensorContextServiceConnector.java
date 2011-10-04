package interdroid.contextdroid.sensors;

import interdroid.contextdroid.ContextServiceConnector;

import java.util.Arrays;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class SensorContextServiceConnector extends ContextServiceConnector {

	public SensorContextServiceConnector(Context context) {
		super(context);
	}

	public void notifyDataChanged(String[] ids) {
		try {
			contextService.notifyDataChanged(ids);
		} catch (RemoteException e) {
			Log.e(getClass().getName(), "Unable to notify context service for ids: " + Arrays.toString(ids));
		}
	}

}
