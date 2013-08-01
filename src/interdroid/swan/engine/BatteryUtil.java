package interdroid.swan.engine;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.R;
import android.content.Context;
import android.content.res.XmlResourceParser;

public class BatteryUtil {
	public static void readPowerProfile(Context context) {
		try {
			int id = (Integer) Class.forName("com.android.internal.R$xml")
					.getField("power_profile").getInt(null);
			XmlResourceParser x = context.getResources().getXml(id);

			String key = null;
			String value = null;

			x.next();
			int eventType = x.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_DOCUMENT) {
				} else if (eventType == XmlPullParser.START_TAG) {
					if (x.getAttributeCount() > 0) {
						key = x.getAttributeValue(0);
					}
//					if (x.getName().equals("array")) {
//						arrays.put(key, new ArrayList<Double>());
//					}
				} else if (eventType == XmlPullParser.END_TAG) {
					if (x.getName().equals("item")) {
						System.out.println(key + ":"
								+ Double.parseDouble(value));
					} else if (x.getName().equals("value")) {
						System.out.println(key + ":"
								+ Double.parseDouble(value));
					}
				} else if (eventType == XmlPullParser.TEXT) {
					value = x.getText();
				}
				eventType = x.next();
			}
		} catch (Throwable t) {
			System.err.println("Power Profile error: " + t.getMessage());
		}
	}
}
