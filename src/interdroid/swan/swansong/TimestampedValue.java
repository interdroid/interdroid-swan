package interdroid.swan.swansong;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A tuple containing a value, a timestamp. Also includes static methods for
 * some calculations on lists of TimestampedValues
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 */
public class TimestampedValue implements Serializable, Parcelable,
		Comparable<TimestampedValue> {

	/**
	 *
	 */
	private static final long serialVersionUID = -1758020216184616414L;

	/** The value. */
	private Object mValue;

	/** The timestamp. */
	private long mTimestamp;

	/**
	 * Construct from a parcel.
	 * 
	 * @param saved
	 *            read from a parcel
	 */
	private TimestampedValue(final Parcel saved) {
		readFromParcel(saved);
	}

	/**
	 * Instantiates a new timestamped value.
	 * 
	 * @param value
	 *            the value
	 */
	public TimestampedValue(final Object value) {
		this(value, System.currentTimeMillis());
	}

	/**
	 * Instantiates a new timestamped value.
	 * 
	 * @param value
	 *            the value
	 * @param timestamp
	 *            the timestamp
	 */
	public TimestampedValue(final Object value, final long timestamp) {
		this.mValue = value;
		this.mTimestamp = timestamp;
	}

	/**
	 * @return the value
	 */
	public final Object getValue() {
		return mValue;
	}

	/**
	 * @return the timestamp
	 */
	public final long getTimestamp() {
		return mTimestamp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "" + mValue + " (timestamp: " + mTimestamp + ")";
	}

	/**
	 * Applies the reduction mode to the values.
	 * 
	 * @param values
	 *            the value to reduce
	 * @return the reduced values.
	 */
	public static TimestampedValue[] applyMode(
			final List<TimestampedValue> values, HistoryReductionMode mode) {
		switch (mode) {
		case MAX:
			return new TimestampedValue[] { TimestampedValue
					.findMaxValue(values) };
		case MIN:
			return new TimestampedValue[] { TimestampedValue
					.findMinValue(values) };
		case MEAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMean(values) };
		case MEDIAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMedian(values) };
		case ALL:
		case ANY:
		default:
			return values.toArray(new TimestampedValue[values.size()]);
		}
	}

	/**
	 * Calculate mean over array of values.
	 * 
	 * @param values
	 *            an array of timestamped values (double or castable to double)
	 * 
	 * @return the mean value, with the oldest timestamp of all values (it will
	 *         be invalid when this timestamp is no longer in the history
	 *         window)
	 */
	public static TimestampedValue calculateMean(
			final List<TimestampedValue> values) {
		double sumValues = 0.0;

		for (TimestampedValue value : values) {
			sumValues += (Double) value.mValue;
		}
		return new TimestampedValue(sumValues / values.size(),
				values.get(0).mTimestamp);
	}

	/**
	 * Calculate a time weighted mean over array of values.
	 * 
	 * @param values
	 *            an array of timestamped values (double or castable to double)
	 * 
	 * @return the time weighted mean value, with the oldest timestamp of all
	 *         values (it will be invalid when this timestamp is no longer in
	 *         the history window)
	 */
	public static TimestampedValue calculateTimeWeightedMean(
			final TimestampedValue[] values) {
		throw new RuntimeException("Not implemented");
	}

	/**
	 * @param values
	 *            the values to search for the median in
	 * @return the median value, with the oldest timestamp of all values (it
	 *         will be invalid when this timestamp is no longer in the history
	 *         window)
	 */
	public static final TimestampedValue calculateMedian(
			final List<TimestampedValue> values) {
		long timestamp = values.get(0).getTimestamp();
		Collections.sort(values);
		TimestampedValue result = values.get(values.size() / 2);
		result.mTimestamp = timestamp;
		return result;
	}

	/**
	 * Find maximum value.
	 * 
	 * @param values
	 *            an array of timestamped values
	 * 
	 * @return the timestamped maximum value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static TimestampedValue findMaxValue(
			final List<TimestampedValue> values) {
		TimestampedValue maxValue = null;
		for (TimestampedValue value : values) {
			if (maxValue == null) {
				maxValue = value;
			} else if (((Comparable) maxValue.mValue).compareTo(value.mValue) < 0) {
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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static TimestampedValue findMinValue(
			final List<TimestampedValue> values) {
		TimestampedValue minValue = null;
		for (TimestampedValue value : values) {
			if (minValue == null) {
				minValue = value;
			} else if (((Comparable) minValue.mValue).compareTo(value.mValue) > 0) {
				minValue = value;
			}
		}
		return minValue;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeLong(mTimestamp);
		dest.writeValue(mValue);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	private void readFromParcel(final Parcel in) {
		mTimestamp = in.readLong();
		mValue = in.readValue(this.getClass().getClassLoader());
	}

	/** The CREATOR. */
	public static final TimestampedValue.Creator<TimestampedValue> CREATOR = new TimestampedValue.Creator<TimestampedValue>() {

		@Override
		public TimestampedValue createFromParcel(final Parcel source) {
			return new TimestampedValue(source);
		}

		@Override
		public TimestampedValue[] newArray(final int size) {
			return new TimestampedValue[size];
		}
	};

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public final int compareTo(final TimestampedValue another) {
		return ((Comparable) mValue)
				.compareTo(((TimestampedValue) another).mValue);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof TimestampedValue) {
			if (mTimestamp != ((TimestampedValue) o).mTimestamp) {
				return false;
			}
			if (mValue instanceof Comparable<?>
					&& ((TimestampedValue) o).mValue instanceof Comparable<?>) {
				if (((Comparable) mValue)
						.compareTo((Comparable) ((TimestampedValue) o).mValue) == 0) {
					return true;
				}
			}
		}
		return super.equals(o);
	}

}
