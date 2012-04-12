package interdroid.swan.contextservice;

import interdroid.swan.SwanException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base exception passed across service interfaces for rethrow
 * on the other side of the service.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class SwanServiceException extends Exception implements
		Parcelable {

	/**
	 * Serial version ID.
	 */
	private static final long serialVersionUID = -632687103119215276L;

	/**
	 * The exception to rethrow.
	 */
	private SwanException mException;

	/**
	 * Constructs an exception with the given inner exception.
	 * @param exception the exception to wrap
	 */
	public SwanServiceException(final SwanException exception) {
		super();
		mException = exception;
	}

	/**
	 * Construct with no inner exception.
	 */
	private SwanServiceException() {
		super();
	}

	/**
	 * @return the inner exception.
	 */
	public final SwanException getSwanException() {
		return mException;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeSerializable(mException);
	}

	/**
	 * Read from parcel.
	 *
	 * @param in
	 *            the in
	 */
	public final void readFromParcel(final Parcel in) {
		mException = (SwanException) in.readSerializable();
	}

	/** The CREATOR. */
	public static final SwanServiceException
	.Creator<SwanServiceException> CREATOR =
	new SwanServiceException.Creator<SwanServiceException>() {

		@Override
		public SwanServiceException createFromParcel(
				final Parcel source) {
			SwanServiceException v = new SwanServiceException();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public SwanServiceException[] newArray(final int size) {
			return new SwanServiceException[size];
		}
	};

}
