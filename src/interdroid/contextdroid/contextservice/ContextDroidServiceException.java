package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextDroidException;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Base exception passed across service interfaces for rethrow
 * on the other side of the service.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public class ContextDroidServiceException extends Exception implements
		Parcelable {

	/**
	 * Serial version ID.
	 */
	private static final long serialVersionUID = -632687103119215276L;

	/**
	 * The exception to rethrow.
	 */
	private ContextDroidException mException;

	/**
	 * Constructs an exception with the given inner exception.
	 * @param exception the exception to wrap
	 */
	public ContextDroidServiceException(final ContextDroidException exception) {
		super();
		mException = exception;
	}

	/**
	 * Construct with no inner exception.
	 */
	private ContextDroidServiceException() {
		super();
	}

	/**
	 * @return the inner exception.
	 */
	public final ContextDroidException getContextDroidException() {
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
		mException = (ContextDroidException) in.readSerializable();
	}

	/** The CREATOR. */
	public static final ContextDroidServiceException
	.Creator<ContextDroidServiceException> CREATOR =
	new ContextDroidServiceException.Creator<ContextDroidServiceException>() {

		@Override
		public ContextDroidServiceException createFromParcel(
				final Parcel source) {
			ContextDroidServiceException v = new ContextDroidServiceException();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public ContextDroidServiceException[] newArray(final int size) {
			return new ContextDroidServiceException[size];
		}
	};

}
