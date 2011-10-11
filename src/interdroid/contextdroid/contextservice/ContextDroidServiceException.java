package interdroid.contextdroid.contextservice;

import interdroid.contextdroid.ContextDroidException;
import android.os.Parcel;
import android.os.Parcelable;

public class ContextDroidServiceException extends Exception implements
		Parcelable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -632687103119215276L;
	private ContextDroidException exception;

	public ContextDroidServiceException(ContextDroidException exception) {
		super();
		this.exception = exception;
	}

	private ContextDroidServiceException() {
		super();
	}

	public ContextDroidException getContextDroidException() {
		return exception;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(exception);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		exception = (ContextDroidException) in.readSerializable();
	}

	/** The CREATOR. */
	public static ContextDroidServiceException.Creator<ContextDroidServiceException> CREATOR = new ContextDroidServiceException.Creator<ContextDroidServiceException>() {

		@Override
		public ContextDroidServiceException createFromParcel(Parcel source) {
			ContextDroidServiceException v = new ContextDroidServiceException();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public ContextDroidServiceException[] newArray(int size) {
			return new ContextDroidServiceException[size];
		}
	};

}
