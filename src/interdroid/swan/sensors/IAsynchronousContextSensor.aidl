package interdroid.swan.sensors;

import interdroid.swan.swansong.TimestampedValue;

interface IAsynchronousContextSensor {

	void register(in String id, in String valuePath, in Bundle configuration);

	void unregister(in String id);

	List<TimestampedValue> getValues(in String id, long now, long timespan);

	String getScheme();
}

