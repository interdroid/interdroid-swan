package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractMemorySensor;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.Bundle;

public class ServerStatusSensor extends AbstractMemorySensor {

	public static final String TAG = "ServerStatus";

	public static final String STATUS_FIELD = "status";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String SERVER_URL = "server_url";
	public static final String CONNECTION_TIMEOUT = "connection_timeout";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;
	public static final int DEFAULT_CONNECTION_TIMEOUT = 3000; // ms

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000;

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
		System.out.println("website sensor connected!");
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		ServerPoller serverPoller = new ServerPoller(id, configuration);
		activeThreads.put(id, serverPoller);
		serverPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class ServerPoller extends Thread {

		private Bundle configuration;
		private List<TimestampedValue> values = new ArrayList<TimestampedValue>();
		private String id;

		ServerPoller(String id, Bundle configuration) {
			this.configuration = configuration;
			this.id = id;
		}

		public void run() {
			int timeout = configuration.getInt(CONNECTION_TIMEOUT,
					mDefaultConfiguration.getInt(CONNECTION_TIMEOUT));
			String serverURL = configuration.getString(SERVER_URL);
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				if (values.size() >= HISTORY_SIZE) {
					values.remove(0);
				}
				values.add(new TimestampedValue(
						sampleStatus(serverURL, timeout), start, start
								+ EXPIRE_TIME));
				notifyDataChangedForId(id);
				try {
					Thread.sleep(configuration.getLong(SAMPLE_INTERVAL,
							mDefaultConfiguration.getLong(SAMPLE_INTERVAL))
							+ start - System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}

		public List<TimestampedValue> getValues() {
			return values;
		}
	}

	@Override
	public void onDestroySensor() {
		for (ServerPoller serverPoller : activeThreads.values()) {
			serverPoller.interrupt();
		}
	};

}
