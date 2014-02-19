package interdroid.swan.swansong;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public class Result implements Comparable<Result>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5680310116890974511L;
	protected TimestampedValue[] mValues;
	protected TriState mTriState;
	protected long mTimestamp;
	protected long mOldestTimestamp;
	protected long mDeferUntil = Long.MAX_VALUE;
	protected boolean mDeferGuaranteed = true;

	public Result(TimestampedValue[] values, long oldestTimestamp) {
		mValues = values;
		mOldestTimestamp = oldestTimestamp;
	}

	public Result(long timestamp, TriState triState) {
		mTriState = triState;
		mTimestamp = timestamp;
	}

	public TimestampedValue[] getValues() {
		return mValues;
	}

	public TriState getTriState() {
		return mTriState;
	}

	public long getDeferUntil() {
		return mDeferUntil;
	}

	public void setDeferUntil(long deferUntil) {
		this.mDeferUntil = deferUntil;
	}

	@Override
	public int compareTo(Result another) {
		if (another == null) {
			return 1;
		}
		long difference = getDeferUntil() - another.getDeferUntil();
		if (difference == 0) {
			return 0;
		} else if (difference < 0) {
			return -1;
		} else {
			return 1;
		}
	}

	public String toString() {
		return (mValues == null ? mTriState : Arrays.toString(mValues))
				+ " until "
				+ (mDeferUntil == 0 ? "NOW"
						: (mDeferUntil == Long.MAX_VALUE ? "FOREVER" : ""
								+ new Date(mDeferUntil)
								+ "." + (System.currentTimeMillis() % 1000)));
	}

	public long getTimestamp() {
		return mTimestamp;
	}

	public long getOldestTimestamp() {
		return mOldestTimestamp;
	}

	public void setDeferUntilGuaranteed(boolean guaranteed) {
		mDeferGuaranteed = guaranteed;
	}

	public boolean isDeferUntilGuaranteed() {
		return mDeferGuaranteed;
	}
}
