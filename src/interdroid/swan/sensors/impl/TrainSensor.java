package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractMemorySensor;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.os.Bundle;
import android.util.Log;

public class TrainSensor extends AbstractMemorySensor {

	public static final String TAG = "Train";

	/**
	 * The configuration activity for this class.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public final int getPreferencesXML() {
			return R.xml.train_preferences;
		}

	}

	public static final String DEPARTURE_TIME_FIELD = "departure_time";
	public static final String ARRIVAL_TIME_FIELD = "arrival_time";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String FROM_STATION = "from_station";
	public static final String TO_STATION = "to_station";
	public static final String DEPARTURE_TIME = "departure_time";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;

	protected static final int HISTORY_SIZE = 10;

	// NS-API specific variables
	private static final String BASE_URL = "http://webservices.ns.nl/ns-api-treinplanner?";
	private static final String BASE64_CREDENTIALS = "ci5kZS5sZWV1d0B2dS5ubDpxOWpLZS1BUmRsVk1kX295NGFhOEZDbGpMZXpIYWg0c0dRT003WFJlU09hRFlFLXNtS2VpWVE=";
	private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
			ISO_8601_DATE_FORMAT, new Locale("nl", "NL"));

	private Map<String, TrainPoller> activeThreads = new HashMap<String, TrainPoller>();

	private Date sampleTrain(String baseURL, int hours, int minutes,
			boolean departure) {
		// construct the URL
		Date date = new Date();// current date
		date.setHours(hours);
		date.setMinutes(minutes);
		date.setSeconds(0);

		try {
			baseURL += URLEncoder.encode(FORMATTER.format(date), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		// String line = null;

		try {

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(baseURL);
			httpGet.setHeader("Authorization", "Basic " + BASE64_CREDENTIALS);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			// line = EntityUtils.toString(httpEntity);
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			Document doc = factory.newDocumentBuilder().parse(
					httpEntity.getContent());
			if ("ReisMogelijkheden".equals(doc.getDocumentElement()
					.getNodeName())) {
				// correct
				NodeList travelOptions = doc
						.getElementsByTagName("ReisMogelijkheid");

				if (departure) {
					return FORMATTER.parse(((Element) travelOptions.item(0))
							.getElementsByTagName("ActueleVertrekTijd").item(0)
							.getFirstChild().getNodeValue());
				} else {
					return FORMATTER.parse(((Element) travelOptions.item(0))
							.getElementsByTagName("ActueleAankomstTijd")
							.item(0).getFirstChild().getNodeValue());
				}
			} else {
				// error
			}

		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "XML error. Unsupported encoding." + e.getMessage());
		} catch (MalformedURLException e) {
			Log.e(TAG, "XML error. Incorrect URL." + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "XML error. IOException." + e.getMessage());
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { DEPARTURE_TIME_FIELD, ARRIVAL_TIME_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
	}

	//@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'train', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ DEPARTURE_TIME_FIELD
				+ "', 'type': 'long'},"
				+ "            {'name': '"
				+ ARRIVAL_TIME_FIELD
				+ "', 'type': 'long'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		TrainPoller trainPoller = new TrainPoller(id, valuePath, configuration);
		activeThreads.put(id, trainPoller);
		trainPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class TrainPoller extends Thread {

		private Bundle configuration;
		private String valuePath;
		private String id;

		TrainPoller(String id, String valuePath, Bundle configuration) {
			this.id = id;
			this.configuration = configuration;
			this.valuePath = valuePath;
		}

		public void run() {
			StringBuilder url = new StringBuilder(BASE_URL);
			url.append("fromStation=");
			url.append(configuration.getString(FROM_STATION));
			url.append("&toStation=");
			url.append(configuration.getString(TO_STATION));
			url.append("&previousAdvices=0");
			url.append("&departure=true&dateTime=");
			boolean departure = DEPARTURE_TIME_FIELD.equals(valuePath);
			int hours = Integer.parseInt(configuration
					.getString(DEPARTURE_TIME).split(":")[0]);
			int minutes = Integer.parseInt(configuration.getString(
					DEPARTURE_TIME).split(":")[1]);

			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				Date date = sampleTrain(url.toString(), hours, minutes,
						departure);
				if (date != null) {
					putValueTrimSize(valuePath, id, start, date, HISTORY_SIZE);
				}
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
		for (TrainPoller trainPoller : activeThreads.values()) {
			trainPoller.interrupt();
		}
	};

}
