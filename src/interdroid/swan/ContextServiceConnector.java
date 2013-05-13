package interdroid.swan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.swan.contextservice.IContextService;
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
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(ContextServiceConnector.class);


	/** The ContextService intent action. */
	public static final String CONTEXT_SERVICE =
			"interdroid.swan.intent.CONTEXTSERVICE";

	/** The context service interface. */
	private IContextService contextService;

	/** The context of the application using the Context Service Connector. */
	private final Context context;

	/** The context manager listener. */
	private ConnectionListener connectionListener;

	/** Is the context service connected? */
	private boolean isConnected = false;

	/**
	 * Checks if the context manager is connected to the context service.
	 *
	 * @return true, if connected
	 */
	public final boolean isConnected() {
		return isConnected;
	}

	/**
	 * Starts the connector.
	 * @see start(ConnectionListener)
	 */
	public final void start() {
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
	 * @param listener
	 *            the connectionListener object to call onConnected() on after
	 *            the context manager has been initialized.
	 */
	public final void start(final ConnectionListener listener) {
		System.out.println("starting connectioListener");
		this.connectionListener = listener;
		getContext().bindService(new Intent(CONTEXT_SERVICE), serviceConnection,
				Service.BIND_AUTO_CREATE);
	}

	/**
	 * Unbind the connection to the Context Service and unregister receivers.
	 * You will generally want to call this method from an Activity's onPause()
	 * method to prevent leaked bindings
	 */
	public final void stop() {
		getContext().unbindService(serviceConnection);
		isConnected = false;
	}

	/**
	 * Shuts down the context service entirely.
	 */
	// TODO: Should we support this even? One app shouldn't be able to
	// shut down the service for another.
	public final void shutdown() {
		try {
			getContextService().shutdown();
		} catch (RemoteException e) {
			LOG.warn("Ignoring exception while shutting down.", e);
		}
	}

	/** The service connection to the ContextService. */
	private final ServiceConnection serviceConnection =
			new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name,
				final IBinder service) {
			System.out.println("Service connected");
			LOG.debug("service connected: {}", name);
			setContextService(IContextService.Stub.asInterface(service));
			isConnected = true;
			if (connectionListener != null) {
				connectionListener.onConnected();
			}
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			LOG.debug("service disconnected");
			setContextService(null);
			isConnected = false;
			if (connectionListener != null) {
				connectionListener.onDisconnected();
			}
		}
	};

	/**
	 * Instantiates a new context service connector.
	 *
	 * @param serviceContext
	 *            the application context
	 */
	public ContextServiceConnector(final Context serviceContext) {
		this.context = serviceContext;
	}

	/**
	 * @return the contextService
	 */
	public final IContextService getContextService() {
		return contextService;
	}

	/**
	 * @param service the contextService to set
	 */
	private void setContextService(final IContextService service) {
		this.contextService = service;
	}

	/**
	 * @return the context
	 */
	public final Context getContext() {
		return context;
	}

}
