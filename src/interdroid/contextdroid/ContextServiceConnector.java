package interdroid.contextdroid;

import interdroid.contextdroid.contextservice.IContextService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The Class ContextServiceConnector.
 */
public abstract class ContextServiceConnector {

	/** Logging tag */
	protected static final String TAG = "ContextServiceConnector";

	/** The ContextService intent action. */
	public static final String CONTEXT_SERVICE = "interdroid.contextdroid.intent.CONTEXTSERVICE";

	/** The context service interface. */
	protected IContextService contextService;

	/** The context of the application using the Context Service Connector. */
	Context context;

	/** The context manager listener. */
	private ConnectionListener connectionListener;

	/** Is the context service connected? */
	boolean isConnected = false;

	/**
	 * Checks if the context manager is connected to the context service.
	 * 
	 * @return true, if connected
	 */
	public boolean isConnected() {
		return isConnected;
	}

	public void start() {
		start(null);
	}

	/**
	 * Non-blocking start.
	 * 
	 * Should always be called before any other calls to the ContextManager.
	 * This method makes sure the Context Service is running and sets up the
	 * listeners required to receive updates from the Context Service.
	 * 
	 * You will generally want to call this method from an Activity's onResume()
	 * method.
	 * 
	 * @param connectionListener
	 *            the connectionListener object to call onConnected() on after
	 *            the context manager has been initialized.
	 */
	public void start(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
		context.bindService(new Intent(CONTEXT_SERVICE), serviceConnection,
				Service.BIND_AUTO_CREATE);
	}

	/**
	 * Unbind the connection to the Context Service and unregister receivers.
	 * You will generally want to call this method from an Activity's onPause()
	 * method to prevent leaked bindings
	 */
	public void stop() {
		context.unbindService(serviceConnection);
		isConnected = false;
	}

	public void shutdown() {
		try {
			contextService.shutdown();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** The service connection to the ContextService. */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			System.out.println("service connected");
			contextService = IContextService.Stub.asInterface(service);
			isConnected = true;
			if (connectionListener != null) {
				connectionListener.onConnected();
			}
		}

		public void onServiceDisconnected(final ComponentName name) {
			System.out.println("service disconnected");
			if (connectionListener != null) {
				connectionListener.onDisconnected();
			}
		}
	};

	/**
	 * Instantiates a new context service connector.
	 * 
	 * @param context
	 *            the application context
	 */
	public ContextServiceConnector(final Context context) {
		this.context = context;
	}

}
