package interdroid.contextdroid.test;

import interdroid.contextdroid.ContextManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.os.Vibrator;

public class TestReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// Intent result = new Intent(context, TestActivity.class);
		// result.setAction(intent.getAction());
		// result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// result.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		// result.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		// result.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		// // result.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		// // result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		// context.startActivity(result);
		if (intent.getAction().equals(ContextManager.ACTION_EXPRESSIONTRUE)) {
			((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
					.vibrate(1000);
		}
		if (intent.getAction().equals(ContextManager.ACTION_NEWREADING)) {
			Parcelable[] objects = (Parcelable[]) intent.getExtras().get(
					"values");
			System.out.println("#" + objects.length);
			for (Parcelable object : objects) {
				System.out.println("- " + object);
			}
		}

	}
}
