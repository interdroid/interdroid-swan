package interdroid.contextdroid.sensors.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

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

import interdroid.cuckoo.PushService;

public class CuckooTrainSensorImpl extends PushService implements
		CuckooTrainSensor {

	// copied from normal sensor
	public static final String DEPARTURE_TIME_FIELD = "departure_time";
	public static final String ARRIVAL_TIME_FIELD = "arrival_time";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String FROM_STATION = "from_station";
	public static final String TO_STATION = "to_station";
	public static final String DEPARTURE_TIME = "departure_time";

	// modified from normal sensor
	public static final long DEFAULT_SAMPLE_INTERVAL = 1 * 60 * 1000;

	// copied from normal sensor
	private static final String BASE_URL = "http://webservices.ns.nl/ns-api-treinplanner?";
	private static final String BASE64_CREDENTIALS = "ci5kZS5sZWV1d0B2dS5ubDpxOWpLZS1BUmRsVk1kX295NGFhOEZDbGpMZXpIYWg0c0dRT003WFJlU09hRFlFLXNtS2VpWVE=";
	private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
			ISO_8601_DATE_FORMAT, new Locale("nl", "NL"));

	private Map<String, TrainPoller> activeThreads = new HashMap<String, TrainPoller>();

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
			Date previous = null;
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				Date date = sampleTrain(url.toString(), hours, minutes,
						departure);
				if (previous == null
						|| (date != null && date.getTime() != previous
								.getTime())) {
					// modified
					if (pushToService(
							"interdroid.contextdroid.sensors.impl.CuckooTrainSensor.ACTION",
							"interdroid.contextdroid.sensors.impl",
							"CuckooTrainSensor", (date.getTime() + ":" + id
									+ ":" + valuePath).getBytes())) {
						System.out.println("success");
						previous = date;
					}
				}
				try {
					Thread.sleep(configuration.getLong(SAMPLE_INTERVAL,
							DEFAULT_SAMPLE_INTERVAL)
							+ start
							- System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}

	}

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

		try {

			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(baseURL);
			httpGet.setHeader("Authorization", "Basic " + BASE64_CREDENTIALS);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public void start(String id, String valuePath,
			android.os.Bundle configuration) throws Exception {
		TrainPoller trainPoller = new TrainPoller(id, valuePath, configuration);
		activeThreads.put(id, trainPoller);
		trainPoller.start();
	}

	public void stop(String id) throws Exception {
		TrainPoller trainPoller = activeThreads.remove(id);
		if (trainPoller != null) {
			trainPoller.interrupt();
		}
	}

}
