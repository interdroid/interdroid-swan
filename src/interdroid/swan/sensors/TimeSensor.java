package interdroid.swan.sensors;

import interdroid.swan.R;
import interdroid.swan.swansong.Comparator;
import interdroid.swan.swansong.Result;
import interdroid.swan.swansong.TriState;

import java.util.Calendar;

import android.os.Bundle;

public class TimeSensor {

	public static final String CURRENT_MS_FIELD = "current";
	public static final String DAY_OF_WEEK_FIELD = "day_of_week";
	public static final String HOUR_OF_DAY_FIELD = "hour_of_day";

	public static final String TAG = "TimeSensor";

	/**
	 * The configuration activity for this sensor.
	 * 
	 * @author nick &lt;palmer@cs.vu.nl&gt;
	 * @author roelof &lt;rkemp@cs.vu.nl&gt;
	 * 
	 */
	public static class ConfigurationActivity extends
			AbstractConfigurationActivity {

		@Override
		public int getPreferencesXML() {
			return R.xml.time_preferences;
		}

	}

	public String[] getValuePaths() {
		return new String[] { CURRENT_MS_FIELD, DAY_OF_WEEK_FIELD,
				HOUR_OF_DAY_FIELD };
	}

	private static boolean compare(Comparator comparator, long a, long b) {
		switch (comparator) {
		case EQUALS:
			return a == b;
		case NOT_EQUALS:
			return a != b;
		case GREATER_THAN:
			return a > b;
		case GREATER_THAN_OR_EQUALS:
			return a >= b;
		case LESS_THAN:
			return a < b;
		case LESS_THAN_OR_EQUALS:
			return a <= b;
		default:
			return true;
		}
	}

	@SuppressWarnings("rawtypes")
	public static Result determineValue(long now, String valuePath,
			Bundle configuration, Comparator comparator, Comparable right) {
		if (CURRENT_MS_FIELD.equals(valuePath)) {
			long asMillis = (Long) right;
			return new Result(now > asMillis ? Long.MAX_VALUE : asMillis,
					compare(comparator, now, asMillis) ? TriState.TRUE
							: TriState.FALSE);

		} else if (HOUR_OF_DAY_FIELD.equals(valuePath)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(now);
			long nowHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
			long rightHourOfDay = (Long) right;
			if (nowHourOfDay < rightHourOfDay) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.HOUR_OF_DAY, (int) rightHourOfDay);
			} else if (nowHourOfDay == rightHourOfDay) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.roll(Calendar.HOUR_OF_DAY, true);
			} else if (nowHourOfDay > rightHourOfDay) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.HOUR_OF_DAY, (int) rightHourOfDay);
				calendar.roll(Calendar.DAY_OF_MONTH, true);
			}
			Result result = new Result(now, compare(comparator, nowHourOfDay,
					rightHourOfDay) ? TriState.TRUE : TriState.FALSE);
			result.setDeferUntil(calendar.getTimeInMillis());
			result.setDeferUntilGuaranteed(true);
			return result;
		} else if (DAY_OF_WEEK_FIELD.equals(valuePath)) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(now);
			long nowDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
			long rightDayOfWeek = (Long) right;
			if (nowDayOfWeek < rightDayOfWeek) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.DAY_OF_WEEK, (int) rightDayOfWeek);
			} else if (nowDayOfWeek == rightDayOfWeek) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.roll(Calendar.DAY_OF_WEEK, true);
			} else if (nowDayOfWeek > rightDayOfWeek) {
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.DAY_OF_WEEK, (int) rightDayOfWeek);
				calendar.roll(Calendar.WEEK_OF_YEAR, true);
			}
			Result result = new Result(now, compare(comparator, nowDayOfWeek,
					rightDayOfWeek) ? TriState.TRUE : TriState.FALSE);
			result.setDeferUntil(calendar.getTimeInMillis());
			result.setDeferUntilGuaranteed(true);
			return result;
		}

		return new Result(now, TriState.UNDEFINED);
	}
}
