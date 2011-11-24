package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;
import interdroid.contextdroid.sensors.IAsynchronousContextSensor;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * This represents a TypedValue where the values come from context.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class ContextTypedValue extends TypedValue {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(ContextTypedValue.class);

	/**
	 * Serial Version ID.
	 */
	private static final long serialVersionUID = 7571963465497645229L;

	/**
	 * The separator used between an entity and a value path
	 */
	public static final String	ENTITY_VALUE_PATH_SEPARATOR	= ":";

	/**
	 * The separator used inside a value path
	 */
	public static final String VALUE_PATH_SEPARATOR = ".";

	/**
	 * Timeout for connection to the sensor.
	 */
	private static final int CONNECTION_TIMEOUT = 1000;

	/** The default history length to keep. 1 second. */
	public static final int	DEFAULT_HISTORY_LENGTH	= 1000;

	/**
	 * ID within the expression.
	 */
	private String mId;

	/**
	 * The entity from which to get values.
	 */
	private String mEntity;

	/**
	 * The value path to get.
	 */
	private String mValuePath;

	/**
	 * The configuration for the sensor.
	 */
	private Bundle mConfiguration = new Bundle();

	/**
	 * The timespan to consider.
	 */
	private long mTimespan;

	/**
	 * Did we fail to register with the sensor?
	 */
	private boolean mRegistrationFailed;

	/**
	 * The sensor to get values from.
	 */
	private IAsynchronousContextSensor mSensor;

	/**
	 * The connection to the sensor service.
	 */
	private ServiceConnection mServiceConnection;

	/**
	 * Construct from a string.
	 * 
	 * @param unparsedContextInfo
	 *            the string to parse
	 */
	public ContextTypedValue(final String unparsedContextInfo) {
		this(unparsedContextInfo, HistoryReductionMode.DEFAULT_MODE,
				DEFAULT_HISTORY_LENGTH);
	}

	/**
	 * Construct from an entity and path.
	 * 
	 * @param entity
	 *            the entity id
	 * @param path
	 *            the value path
	 */
	public ContextTypedValue(final String entity, final String path) {
		this(entity, path, null, HistoryReductionMode.DEFAULT_MODE,
				DEFAULT_HISTORY_LENGTH);
	}

	/**
	 * Construct from an entity and path and config map.
	 * 
	 * @param entity
	 *            the entity id
	 * @param path
	 *            the value path
	 * @param config
	 *            the map with configuration data
	 */
	public ContextTypedValue(final String entity, final String path,
			final Map<String, String> config) {
		this(entity, path, config, HistoryReductionMode.DEFAULT_MODE,
				DEFAULT_HISTORY_LENGTH);
	}

	/**
	 * Construct from an entity and path and config map with mode and timespan.
	 * 
	 * @param entity
	 *            the entity id
	 * @param path
	 *            the value path
	 * @param mode
	 *            the history reduction mode
	 * @param timespan
	 *            the timespan to limit to
	 */
	public ContextTypedValue(final String entity, final String path,
			final HistoryReductionMode mode, final long timespan) {
		this(entity, path, null, mode, timespan);
	}

	/**
	 * Construct from an entity and path and config map.
	 * 
	 * @param entity
	 *            the entity id
	 * @param path
	 *            the value path
	 * @param config
	 *            the map with configuration data
	 * @param mode
	 *            the history reduction mode
	 * @param timespan
	 *            the timespan to limit to
	 */
	public ContextTypedValue(final String entity, final String path,
			final Map<String, String> config, final HistoryReductionMode mode,
			final long timespan) {
		super(mode);
		mEntity = entity;
		mValuePath = path;
		setHistoryTimespan(timespan);
		if (config != null) {
			for (Entry<String, String> entry : config.entrySet()) {
				mConfiguration.putString(entry.getKey(), entry.getValue());
			}
		}
	}

	/**
	 * Sets the timespan of history to keep.
	 * This sets to DEFAULT_HISTORY_LENGTH if timespan <= 0
	 * @param timespan the timespan to set to.
	 */
	private void setHistoryTimespan(final long timespan) {
		if (timespan > 0) {
			mTimespan = timespan;
		} else {
			mTimespan = DEFAULT_HISTORY_LENGTH;
		}
	}

	/**
	 * Construct with a specific history mode and timespan.
	 * 
	 * @param unparsedContextInfo
	 *            the string to parse
	 * @param mode
	 *            the mode to run with
	 * @param timespan
	 *            the timespan to consider
	 */
	public ContextTypedValue(final String unparsedContextInfo,
			final HistoryReductionMode mode, final long timespan) {
		super(mode);
		String[] splitOnEntity = unparsedContextInfo.split("/", 2);
		this.mEntity = splitOnEntity[0];
		if (splitOnEntity.length != 2) {
			throw new RuntimeException("bad id: '" + unparsedContextInfo
					+ "' no valuepath");
		}
		String[] splitOnValuePath = splitOnEntity[1].split("\\?", 2);
		this.mValuePath = splitOnValuePath[0];
		if (splitOnValuePath.length == 2) {
			String[] splitOnConfigurationItems = splitOnValuePath[1]
					.split("\\&");
			if (splitOnConfigurationItems != null
					&& splitOnConfigurationItems.length > 0) {
				for (String configurationItem : splitOnConfigurationItems) {
					if (!configurationItem.contains("=")) {
						continue;
					}
					mConfiguration.putString(configurationItem.split("=")[0],
							configurationItem.split("=", 2)[1]);
				}
			}
		}

		setHistoryTimespan(timespan);
	}

	/**
	 * Construct from a Parcel.
	 * 
	 * @param source
	 *            the Parcel to read from
	 */
	public ContextTypedValue(final Parcel source) {
		super(source);
		readFromParcel(source);
	}

	/**
	 * @return the entity for this value.
	 */
	public final String getEntity() {
		return mEntity;
	}

	/**
	 * @return the value path for this value.
	 */
	public final String getValuePath() {
		return mValuePath;
	}

	/**
	 * @return the configuration for this value.
	 */
	public final Bundle getConfiguration() {
		return mConfiguration;
	}

	/**
	 * @return the id for this value in the expression.
	 */
	public final String getId() {
		return mId;
	}

	@Override
	public final TimestampedValue[] getValues(final String id, final long now)
			throws ContextDroidException {
		if (mSensor == null) {
			// wait a little and try again, otherwise throw exception
			try {
				Thread.sleep(CONNECTION_TIMEOUT);
			} catch (InterruptedException e) {
				LOG.error("Interrupted while waiting for conncetion.");
			}
			if (mSensor == null) {
				throw new ContextDroidException("Failed to bind to sensor in "
						+ CONNECTION_TIMEOUT + " ms.");
			}
		}
		if (mRegistrationFailed) {
			throw new ContextDroidException("Failed to register " + toString()
					+ " to sensor");
		}
		List<TimestampedValue> values;
		try {
			values = mSensor.getValues(id, now, mTimespan);
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		}

		// If the previous step didn't result in any readings, there's nothing
		// to evaluate, so the expression's value is undefined.
		if (values.size() == 0) {
			LOG.debug("No readings so returning null.");
			throw new NoValuesInIntervalException("No values in interval for "
					+ this);
		}

		// apply modes on the values
		TimestampedValue[] result = applyMode(values
				.toArray(new TimestampedValue[values.size()]));
		return result;
	}

	@Override
	public final int describeContents() {
		return 0;
	}

	@Override
	protected final void writeSubclassToParcel(final Parcel dest,
			final int flags) {
		dest.writeString(mEntity);
		dest.writeString(mValuePath);
		dest.writeLong(mTimespan);
		dest.writeBundle(mConfiguration);
	}

	/**
	 * Read from parcel.
	 * 
	 * @param in
	 *            the in
	 */
	private void readFromParcel(final Parcel in) {
		mEntity = in.readString();
		mValuePath = in.readString();
		mTimespan = in.readLong();
		mConfiguration = in.readBundle();
	}

	/** The CREATOR. */
	public static final ContextTypedValue.Creator<ContextTypedValue> CREATOR = new ContextTypedValue.Creator<ContextTypedValue>() {

		@Override
		public ContextTypedValue createFromParcel(final Parcel source) {
			ContextTypedValue v = new ContextTypedValue(source);
			return v;
		}

		@Override
		public ContextTypedValue[] newArray(final int size) {
			return new ContextTypedValue[size];
		}
	};

	@Override
	public final void initialize(final String id,
			final SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		this.mId = id;
		mServiceConnection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(final ComponentName name) {
				mSensor = null;
			}

			@Override
			public void onServiceConnected(final ComponentName name,
					final IBinder service) {
				IAsynchronousContextSensor sensor = IAsynchronousContextSensor.Stub
						.asInterface(service);
				try {
					sensor.register(id, mValuePath, mConfiguration);
					mRegistrationFailed = false;
				} catch (RemoteException e) {
					LOG.error("Registration failed!", e);
					mRegistrationFailed = true;
				}
				ContextTypedValue.this.mSensor = sensor;
			}
		};

		sensorManager.bindToSensor(this, mServiceConnection);
	}

	@Override
	public final void destroy(final String id, final SensorManager sensorManager)
			throws ContextDroidException {
		try {
			mSensor.unregister(id);
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		}
		LOG.debug("unbind sensor service from context typed value: {}", this);
		sensorManager.unbindSensor(mServiceConnection);
		mSensor = null;
	}

	@Override
	public final boolean hasCurrentTime() {
		return "time".equals(mEntity) && "current".equals(mValuePath);
	}

	@Override
	public final String toString() {
		return mEntity + ":" + mValuePath; // + ": " + configuration;
	}

	@Override
	public final String toParseString() {
		return mEntity + ":" + mValuePath + getParseConfig() + getModeString();
	}

	/**
	 * @return a parseable string for the mode we are running in.
	 */
	private String getModeString() {
		String ret;
		if (!(getHistoryReductionMode().equals(HistoryReductionMode.DEFAULT_MODE)
				&& mTimespan == 0)) {
			ret = " {" + getHistoryReductionMode().toParseString() + ","
					+ mTimespan + "}";
		} else {
			ret = "";
		}
		return ret;
	}

	/**
	 * 
	 * @return a string with the configuration for this value.
	 */
	private String getParseConfig() {
		String ret;
		if (mConfiguration.size() > 0) {
			StringBuffer buf = new StringBuffer('?');
			boolean first = true;
			for (String key : mConfiguration.keySet()) {
				if (!first) {
					buf.append("&");
					first = false;
				}
				buf.append(key);
				buf.append("=");
				buf.append(mConfiguration.getString(key));
			}
			ret = buf.toString();
		} else {
			ret = "";
		}
		return ret;
	}

	/**
	 * Sets the id for this TypedValue.
	 * 
	 * @param id
	 *            the id to set to
	 */
	public final void setId(final String id) {
		mId = id;
	}

	@Override
	public boolean isConstant() {
		return false;
	}

	/**
	 * @return the timespan for the history.
	 */
	public long getHistoryLength() {
		// TODO: Rename to mHistoryLength
		return mTimespan;
	}
}
