package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;
import interdroid.contextdroid.sensors.AbstractMemorySensor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;

public class ServerStatusSensor extends AbstractMemorySensor {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ServerStatusSensor.class);

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.serverstatus_preferences;
		}

	}

	public static final String STATUS_FIELD = "status";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String SERVER_URL = "server_url";
	public static final String CONNECTION_TIMEOUT = "connection_timeout";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;
	public static final int DEFAULT_CONNECTION_TIMEOUT = 3000; // ms

	protected static final int HISTORY_SIZE = 10;

	private Map<String, ServerPoller> activeThreads = new HashMap<String, ServerPoller>();

	private int sampleStatus(String serverURL, int connectionTimeOut) {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams
				.setConnectionTimeout(httpParams, connectionTimeOut);
		HttpConnectionParams.setSoTimeout(httpParams, connectionTimeOut);
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
		HttpGet httpGet = new HttpGet(serverURL);
		try {
			return httpClient.execute(httpGet).getStatusLine().getStatusCode();
		} catch (ClientProtocolException e) {
			return -1;
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { STATUS_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
		DEFAULT_CONFIGURATION.putInt(CONNECTION_TIMEOUT,
				DEFAULT_CONNECTION_TIMEOUT);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'server_status', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ STATUS_FIELD
				+ "', 'type': 'int'}" + "           ]" + "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		LOG.debug("website sensor connected!");
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		ServerPoller serverPoller = new ServerPoller(id, valuePath,
				configuration);
		activeThreads.put(id, serverPoller);
		serverPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class ServerPoller extends Thread {

		private String id;
		private Bundle configuration;
		private String valuePath;

		ServerPoller(String id, String valuePath, Bundle configuration) {
			this.id = id;
			this.configuration = configuration;
			this.valuePath = valuePath;
		}

		public void run() {
			int timeout = configuration.getInt(CONNECTION_TIMEOUT,
					mDefaultConfiguration.getInt(CONNECTION_TIMEOUT));
			String serverURL = configuration.getString(SERVER_URL);
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				putValueTrimSize(valuePath, id, start,
						sampleStatus(serverURL, timeout), HISTORY_SIZE);
				try {
					Thread.sleep(configuration.getLong(SAMPLE_INTERVAL,
							mDefaultConfiguration.getLong(SAMPLE_INTERVAL))
							+ start - System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}

	}

	@Override
	public void onDestroySensor() {
		for (ServerPoller serverPoller : activeThreads.values()) {
			serverPoller.interrupt();
		}
	};

}
