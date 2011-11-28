package interdroid.contextdroid.test;

import interdroid.contextdroid.ContextManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.os.Vibrator;

public class TestReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		File locationLogFile = new File("/sdcard/locationlog.log");
		try {
			if (!locationLogFile.exists()) {

				locationLogFile.createNewFile();
			}

			if (intent.getAction().equals(ContextManager.ACTION_NEWREADING)) {
				((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE))
						.vibrate(1000);

				Parcelable[] objects = (Parcelable[]) intent.getExtras().get(
						"values");
				FileWriter writer = new FileWriter(locationLogFile, true);
				writer.write("time: " + new Date().toLocaleString() + ": ");
				writer.write(objects[0].toString() + "\n");
				writer.flush();
				writer.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// Intent result = new Intent(context, TestActivity.class);
	// result.setAction(intent.getAction());
	// result.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	// result.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	// result.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
	// result.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	// // result.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
	// // result.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	// context.startActivity(result);

}
