package interdroid.swan.crossdevice;

import java.io.IOException;

import android.util.Log;

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class Pusher {

	public static final String TAG = "SWAN Pusher";

	public static void push(String toRegistrationId, String expressionId,
			String action, String data) {
		push(null, toRegistrationId, expressionId, action, data);
	}

	public static void push(final String fromRegistrationId,
			final String toRegistrationId, final String expressionId,
			final String action, final String data) {
		new Thread() {
			public void run() {
				Sender sender = new Sender(SwanGCMConstants.API_KEY);
				Message.Builder builder = new Message.Builder();
				builder.timeToLive(60 * 60).collapseKey("MAGIC_STRING")
						.delayWhileIdle(true);
				if (fromRegistrationId != null) {
					// from is not allowed and results in InvalidDataKey, see:
					// http://developer.android.com/google/gcm/gcm.html
					builder.addData("source", fromRegistrationId);
				}
				builder.addData("action", action);
				builder.addData("data", data);
				builder.addData("id", expressionId);
				Message message = builder.build();
				try {
					Result result = sender.send(message, toRegistrationId, 5);
					if (result.getMessageId() != null) {
						String canonicalRegId = result
								.getCanonicalRegistrationId();
						if (canonicalRegId != null) {
							Log.d(TAG,
									"same device has more than on registration ID: update database");
						} else {
							Log.d(TAG,
									"successfully sent push message for id: "
											+ expressionId + ", type: "
											+ action + ", data: " + data);
						}
					} else {
						String error = result.getErrorCodeName();
						if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
							Log.d(TAG,
									"application has been removed from device - unregister database");
						} else {
							Log.d(TAG, "no message id, error: " + error);
						}
					}
				} catch (IOException e) {
					Log.d(TAG,
							"failed to deliver push message: " + e.toString());
				}

			}
		}.start();
	}
}
