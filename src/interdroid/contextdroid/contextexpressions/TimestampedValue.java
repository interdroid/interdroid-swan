package interdroid.contextdroid.contextexpressions;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * A tuple containing a value, a timestamp and an expire time. Also includes
 * static methods for some calculations on lists of TimestampedValues
 */
public class TimestampedValue implements Serializable, Parcelable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1758020216184616414L;

	/** The value. */
	public Object value;

	/** The timestamp. */
	public long timestamp;

	/** The expire time. */
	public long expireTime;

	private TimestampedValue() {

	}

	/**
	 * Instantiates a new timestamped value.
	 * 
	 * @param value
	 *            the value
	 */
	public TimestampedValue(final Object value) {
		this(value, System.currentTimeMillis(), 0);
	}

	/**
	 * Instantiates a new timestamped value.
	 * 
	 * @param value
	 *            the value
	 * @param timestamp
	 *            the timestamp
	 */
	public TimestampedValue(final Object value, long timestamp) {
		this(value, timestamp, 0);
	}

	/**
	 * Instantiates a new timestamped value.
	 * 
	 * @param value
	 *            the value
	 * @param timestamp
	 *            the timestamp
	 * @param expireTime
	 *            the expire time
	 */
	public TimestampedValue(final Object value, long timestamp, long expireTime) {
		this.value = value;
		this.timestamp = timestamp;
		this.expireTime = expireTime;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "" + value + " (timestamp: " + timestamp + ", expires: "
				+ expireTime + ", difference: " + (expireTime - timestamp);
	}

	/**
	 * Calculate mean over interval.
	 * 
	 * @param values
	 *            an array of timestamped values (double or castable to double)
	 * @param start
	 *            the start of the interval
	 * @param end
	 *            the end of the interval
	 * 
	 * @return the mean value, with the mean timestamp, and the mean expiretime
	 */
	@SuppressWarnings("unchecked")
	public static TimestampedValue calculateMean(TimestampedValue[] values) {
		double sumValues = 0.0;
		long sumTimestamps = 0;
		long sumExpireTimes = 0;
		long totalMs = 0;
		long readingLength = 0;

		for (int i = 0; i < values.length; i++) {
			readingLength = Math.min(values[i].expireTime,
					(i < values.length - 1) ? values[i + 1].timestamp
							: values[i].expireTime)
					- values[i].timestamp;
			totalMs += readingLength;
			sumValues += readingLength * (Double) values[i].value;
			sumTimestamps += readingLength * values[i].timestamp;
			sumExpireTimes += readingLength * values[i].expireTime;
		}
		Log.d("TimestampedValue", "Mean: sum = " + sumValues + " totalMs = "
				+ totalMs + " mean: " + (sumValues / totalMs));
		return new TimestampedValue(sumValues / totalMs, sumTimestamps
				/ totalMs, sumExpireTimes / totalMs);
	}

	public static TimestampedValue calculateMedian(TimestampedValue[] values) {
		throw new RuntimeException("Not implemented yet");
	}

	/**
	 * Find maximum value.
	 * 
	 * @param values
	 *            an array of timestamped values
	 * 
	 * @return the timestamped maximum value
	 */
	@SuppressWarnings("unchecked")
	public static TimestampedValue findMaxValue(TimestampedValue[] values) {
		TimestampedValue maxValue = null;
		for (TimestampedValue value : values) {
			if (maxValue == null) {
				maxValue = value;
			} else if (((Comparable) maxValue.value).compareTo(value.value) < 0) {
				maxValue = value;
			}
		}
		return maxValue;
	}

	/**
	 * Find mininimum value.
	 * 
	 * @param values
	 *            an array of timestamped values
	 * 
	 * @return the timestamped minimum value
	 */
	@SuppressWarnings("unchecked")
	public static TimestampedValue findMinValue(TimestampedValue[] values) {
		TimestampedValue minValue = null;
		for (TimestampedValue value : values) {
			if (minValue == null) {
				minValue = value;
			} else if (((Comparable) minValue.value).compareTo(value.value) > 0) {
				minValue = value;
			}
		}
		return minValue;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(timestamp);
		dest.writeLong(expireTime);
		dest.writeValue(value);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		timestamp = in.readLong();
		expireTime = in.readLong();
		value = in.readValue(this.getClass().getClassLoader());
	}

	/** The CREATOR. */
	public static TimestampedValue.Creator<TimestampedValue> CREATOR = new TimestampedValue.Creator<TimestampedValue>() {

		@Override
		public TimestampedValue createFromParcel(Parcel source) {
			TimestampedValue t = new TimestampedValue();
			t.readFromParcel(source);
			return t;
		}

		@Override
		public TimestampedValue[] newArray(int size) {
			return new TimestampedValue[size];
		}
	};

}
