package interdroid.swan.crossdevice;

import interdroid.swan.engine.EvaluationEngineService;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class CrossDeviceReceiver extends BroadcastReceiver {

	public static final String TAG = "SWAN-CrossDevice";

	@Override
	public void onReceive(Context context, Intent intent) {
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
		String messageType = gcm.getMessageType(intent);
		if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
			Log.d(TAG, "Received message but encountered send error.");
		} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
				.equals(messageType)) {
			Log.d(TAG, "Messages were deleted at the server.");
		} else {
			// forward this intent to evaluation engine service
			intent.setClass(context, EvaluationEngineService.class);
			intent.setAction(intent.getStringExtra("action"));
			Log.d(TAG, "Forwarding intent to evaluation engine: " + intent);
			context.startService(intent);
		}
		setResultCode(Activity.RESULT_OK);

	}

}
