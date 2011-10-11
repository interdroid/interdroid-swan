package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractAsynchronousSensor;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

public class TrainSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "Train";

	public static final String DEPARTURE_TIME_FIELD = "departure_time";
	public static final String ARRIVAL_TIME_FIELD = "arrival_time";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String FROM_STATION = "from_station";
	public static final String TO_STATION = "to_station";
	public static final String DEPARTURE_HOURS = "departure_hours";
	public static final String DEPARTURE_MINUTES = "departure_minutes";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000;

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

	public void onDestroy() {
		for (TrainPoller trainPoller : activeThreads.values()) {
			trainPoller.interrupt();
		}
		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { DEPARTURE_TIME_FIELD, ARRIVAL_TIME_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
	}

	@Override
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
	protected void register(String id, String valuePath, Bundle configuration) {
		TrainPoller trainPoller = new TrainPoller(id, valuePath, configuration);
		activeThreads.put(id, trainPoller);
		trainPoller.start();
	}

	@Override
	protected void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	@Override
	protected List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		return getValuesForTimeSpan(activeThreads.get(id).getValues(), now,
				timespan);
	}

	class TrainPoller extends Thread {

		private Bundle configuration;
		private List<TimestampedValue> values = new ArrayList<TimestampedValue>();
		private String id;
		private String valuePath;

		TrainPoller(String id, String valuePath, Bundle configuration) {
			this.configuration = configuration;
			this.valuePath = valuePath;
			this.id = id;
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

			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				if (values.size() >= HISTORY_SIZE) {
					values.remove(0);
				}
				Date date = sampleTrain(url.toString(),
						configuration.getInt(DEPARTURE_HOURS),
						configuration.getInt(DEPARTURE_MINUTES), departure);
				if (date != null) {
					values.add(new TimestampedValue(date, start, start
							+ EXPIRE_TIME));
					notifyDataChangedForId(id);
				}
				try {
					Thread.sleep(configuration.getLong(SAMPLE_INTERVAL,
							DEFAULT_CONFIGURATION.getLong(SAMPLE_INTERVAL))
							+ start - System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}

		public List<TimestampedValue> getValues() {
			return values;
		}
	};

}
