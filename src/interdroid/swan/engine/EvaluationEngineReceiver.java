package interdroid.swan.engine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EvaluationEngineReceiver extends BroadcastReceiver {

	/**
	 * This receiver acts as a forwarder to the EvaluationEngineService.
	 * 
	 * We don't want 3rd party applications to be able to invoke
	 * Context.stopService(Intent) on the EvaluationEngineService, because that
	 * will stop evaluations of expressions for other applications too.
	 * Therefore the EvaluationEngineService is not public (e.g. not exported),
	 * hence it cannot be started and stopped from outside the apk. However, we
	 * allow intents to be sent to this receiver who will then forward it to the
	 * service, but always using the Context.startService(Intent), rather than
	 * the Context.stopService(Intent).
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// forward the intent to the service
		intent.setClass(context, EvaluationEngineService.class);
		context.startService(intent);
	}

}
