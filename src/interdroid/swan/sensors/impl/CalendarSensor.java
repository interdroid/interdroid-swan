package interdroid.swan.sensors.impl;

import interdroid.swan.R;
import interdroid.swan.sensors.AbstractConfigurationActivity;
import interdroid.swan.sensors.AbstractVdbSensor;
import interdroid.swan.swansong.TimestampedValue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

public class CalendarSensor extends AbstractVdbSensor {

	public static final String TAG = "Calendar";

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
			return R.xml.calendar_preferences;
		}

	}

	public static final String START_TIME_NEXT_EVENT_FIELD = "start_time_next_event";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String IGNORE_FREE_EVENTS = "ignore_free_events";
	public static final String IGNORE_ALLDAY_EVENTS = "ignore_allday_events";
	/**
	 * Get your private calendar URL from Google Calendar in the browser, click
	 * on small arrow next to a given calendar, choose calendar settings, get
	 * the address from private address (xml), without the ending '/basic'
	 */
	public static final String PRIVATE_CALENDAR_URL = "private_calendar_url";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;
	public static final boolean DEFAULT_IGNORE_FREE_EVENTS = true;
	public static final boolean DEFAULT_IGNORE_ALLDAY_EVENTS = true;

	protected static final int HISTORY_SIZE = 10;

	// Google Calendar specific variables

	private static final String FREE_EVENT = "http://schemas.google.com/g/2005#event.transparent";
	private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private static final String ISO_8601_DATE_FORMAT_DAY = "yyyy-MM-dd";

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat(
			ISO_8601_DATE_FORMAT, new Locale("nl", "NL"));
	private static final SimpleDateFormat FORMATTER_DAY = new SimpleDateFormat(
			ISO_8601_DATE_FORMAT_DAY, new Locale("nl", "NL"));

	private Map<String, CalendarPoller> activeThreads = new HashMap<String, CalendarPoller>();

	private Date sampleCalendar(String privateCalendarUrl,
			boolean ignoreAllDay, boolean ignoreFree) {
		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(privateCalendarUrl);
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			Document doc = factory.newDocumentBuilder().parse(
					httpEntity.getContent());

			Element root = doc.getDocumentElement();
			NodeList items = root.getElementsByTagName("entry");

			for (int i = 0; i < items.getLength(); i++) {
				NodeList properties = items.item(i).getChildNodes();
				Date result = null;
				for (int j = 0; j < properties.getLength(); j++) {
					Element element = (Element) properties.item(j);
					String name = element.getNodeName();
					if ("gd:when".equals(name)) {
						try {
							result = FORMATTER.parse(element
									.getAttribute("startTime"));
						} catch (ParseException e) {
							// cannot be parsed, must be an allday event
							if (ignoreAllDay) {
								continue;
							} else {
								// parse it as an allday event
								result = FORMATTER_DAY.parse(element
										.getAttribute("startTime"));
							}
						}
					}
					if ("gd:transparency".equals(name)) {
						if (ignoreFree
								&& element.getAttribute("value").equals(
										FREE_EVENT)) {
							continue;
						}
					}

				}
				if (result != null) {
					return result;
				}
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
		return new String[] { START_TIME_NEXT_EVENT_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
		DEFAULT_CONFIGURATION.putBoolean(IGNORE_ALLDAY_EVENTS,
				DEFAULT_IGNORE_ALLDAY_EVENTS);
		DEFAULT_CONFIGURATION.putBoolean(IGNORE_FREE_EVENTS,
				DEFAULT_IGNORE_FREE_EVENTS);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'train', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ START_TIME_NEXT_EVENT_FIELD
				+ "', 'type': 'date'}"
				+ "           ]" + "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public final void register(String id, String valuePath, Bundle configuration) {
		CalendarPoller calendarPoller = new CalendarPoller(id, configuration);
		activeThreads.put(id, calendarPoller);
		calendarPoller.start();
	}

	@Override
	public final void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	class CalendarPoller extends Thread {

		private Bundle configuration;
		private List<TimestampedValue> values = new ArrayList<TimestampedValue>();
		private String id;

		CalendarPoller(String id, Bundle configuration) {
			this.configuration = configuration;
			this.id = id;
		}

		public void run() {
			boolean ignoreFreeEvents = configuration.getBoolean(
					IGNORE_FREE_EVENTS,
					mDefaultConfiguration.getBoolean(IGNORE_FREE_EVENTS));
			boolean ignoreAlldayEvents = configuration.getBoolean(
					IGNORE_ALLDAY_EVENTS,
					mDefaultConfiguration.getBoolean(IGNORE_ALLDAY_EVENTS));
			String privateCalendarURL = configuration
					.getString(PRIVATE_CALENDAR_URL)
					+ "/full/?max-results=5&singleevents=true&futureevents=true&orderby=starttime&sortorder=a";

			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				if (values.size() >= HISTORY_SIZE) {
					values.remove(0);
				}
				Date date = sampleCalendar(privateCalendarURL,
						ignoreAlldayEvents, ignoreFreeEvents);
				if (date != null) {
					values.add(new TimestampedValue(date, start));
					notifyDataChangedForId(id);
				}
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
		for (CalendarPoller calendarPoller : activeThreads.values()) {
			calendarPoller.interrupt();
		}
	}

}
