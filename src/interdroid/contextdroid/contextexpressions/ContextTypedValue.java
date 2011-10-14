package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorInitializationFailedException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.sensors.IAsynchronousContextSensor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class ContextTypedValue extends TypedValue implements
		Comparable<ContextTypedValue> {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(ContextTypedValue.class);

	/**
	 *
	 */
	private static final long serialVersionUID = 7571963465497645229L;

	private static final int CONNECTION_TIMEOUT = 1000;

	private static final String TAG = "ContextTypedValue";

	String id;
	String entity;
	String valuePath;
	Bundle configuration = new Bundle();
	long timespan;
	long deferUntil;

	boolean registrationFailed;

	private IAsynchronousContextSensor sensor;

	private ServiceConnection serviceConnection;

	public ContextTypedValue(String unparsedContextInfo) {
		this(unparsedContextInfo, HistoryReductionMode.NONE, 0);
	}

	public String getEntity() {
		return entity;
	}

	public String getValuePath() {
		return valuePath;
	}

	public Bundle getConfiguration() {
		return configuration;
	}

	public String getId() {
		return id;
	}

	public ContextTypedValue(String unparsedContextInfo,
			HistoryReductionMode mode, long timespan) {
		String[] splitOnEntity = unparsedContextInfo.split("/", 2);
		this.entity = splitOnEntity[0];
		if (splitOnEntity.length != 2) {
			throw new RuntimeException("bad id: '" + unparsedContextInfo
					+ "' no valuepath");
		}
		String[] splitOnValuePath = splitOnEntity[1].split("\\?", 2);
		this.valuePath = splitOnValuePath[0];
		if (splitOnValuePath.length == 2) {
			String[] splitOnConfigurationItems = splitOnValuePath[1]
					.split("\\&");
			if (splitOnConfigurationItems != null
					&& splitOnConfigurationItems.length > 0) {
				for (String configurationItem : splitOnConfigurationItems) {
					if (!configurationItem.contains("=")) {
						continue;
					}
					configuration.putString(configurationItem.split("=")[0],
							configurationItem.split("=", 2)[1]);
				}
			}
		}

		this.mode = mode;
		this.timespan = timespan;
	}

	private ContextTypedValue() {
	}

	@Override
	public TimestampedValue[] getValues(String id, long now)
			throws ContextDroidException, NoValuesInIntervalException {
		if (sensor == null) {
			// wait a little and try again, otherwise throw exception
			try {
				Thread.sleep(CONNECTION_TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}
			if (sensor == null) {
				throw new ContextDroidException("Failed to bind to sensor in "
						+ CONNECTION_TIMEOUT + " ms.");
			}
		}
		if (registrationFailed) {
			throw new ContextDroidException("Failed to register " + toString()
					+ " to sensor");
		}
		List<TimestampedValue> values;
		try {
			values = sensor.getValues(id, now, timespan);
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		}

		// If the previous step didn't result in any readings, there's nothing
		// to evaluate, so the expression's value is undefined.
		if (values.size() == 0) {
			Log.d(TAG, "No readings so returning null.");
			deferUntil = Long.MIN_VALUE;
			throw new NoValuesInIntervalException("No values in interval for "
					+ this);
		}

		// apply modes on the values
		TimestampedValue[] result = applyMode(values
				.toArray(new TimestampedValue[values.size()]));
		// set the defer time
		deferUntil = result[result.length - 1].expireTime;
		return result;
	}

	@Override
	public long deferUntil() {
		return deferUntil;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(entity);
		dest.writeString(valuePath);
		dest.writeLong(timespan);
		dest.writeLong(deferUntil);
		dest.writeBundle(configuration);
		dest.writeInt(mode.ordinal());
	}

	/**
	 * Read from parcel.
	 *
	 * @param in
	 *            the in
	 */
	public void readFromParcel(Parcel in) {
		entity = in.readString();
		valuePath = in.readString();
		timespan = in.readLong();
		deferUntil = in.readLong();
		configuration = in.readBundle();
		mode = HistoryReductionMode.values()[in.readInt()];
	}

	/** The CREATOR. */
	public static ContextTypedValue.Creator<ContextTypedValue> CREATOR = new ContextTypedValue.Creator<ContextTypedValue>() {

		@Override
		public ContextTypedValue createFromParcel(Parcel source) {
			ContextTypedValue v = new ContextTypedValue();
			v.readFromParcel(source);
			return v;
		}

		@Override
		public ContextTypedValue[] newArray(int size) {
			return new ContextTypedValue[size];
		}
	};

	@Override
	public void initialize(final String id, SensorManager sensorManager)
			throws SensorConfigurationException,
			SensorInitializationFailedException {
		this.id = id;
		serviceConnection = new ServiceConnection() {

			@Override
			public void onServiceDisconnected(ComponentName name) {
				sensor = null;
			}

			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				IAsynchronousContextSensor sensor = IAsynchronousContextSensor.Stub
						.asInterface(service);
				try {
					sensor.register(id, valuePath, configuration);
					registrationFailed = false;
				} catch (RemoteException e) {
					LOG.error("Registration failed!", e);
					registrationFailed = true;
				}
				ContextTypedValue.this.sensor = sensor;
			}
		};

		sensorManager.bindToSensor(this, serviceConnection);
	}

	@Override
	public void destroy(final String id, SensorManager sensorManager)
			throws ContextDroidException {
		try {
			sensor.unregister(id);
		} catch (RemoteException e) {
			throw new ContextDroidException(e);
		}
		LOG.debug("unbind sensor service from context typed value: {}", this);
		sensorManager.unbindSensor(serviceConnection);
		sensor = null;
	}

	@Override
	public boolean hasCurrentTime() {
		return "time".equals(entity) && "current".equals(valuePath);
	}

	public String toString() {
		return entity + "/" + valuePath;// + ": " + configuration;
	}

	public void setNextEvaluationTime(long nextEvaluationTime) {
		deferUntil = nextEvaluationTime;
	}

	@Override
	public int compareTo(ContextTypedValue another) {
		long difference = deferUntil - another.deferUntil;
		if (difference == 0) {
			return 0;
		} else if (difference < 0) {
			return -1;
		} else {
			return 1;
		}
	}
}
