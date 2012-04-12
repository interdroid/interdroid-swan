package interdroid.swan.ui;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity that launches the ContextService.
 */
public class LaunchService extends Activity {


	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		Log.d("LaunchService", "thread: " + Thread.currentThread().getId()
//				+ " - " + Thread.currentThread().getName()
//				+ " Process: " + android.os.Process.myPid());
//		startService(new Intent(ContextManager.CONTEXT_SERVICE));
		finish();
	}

}
