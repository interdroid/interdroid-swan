package interdroid.contextdroid.sensors.impl;

interface CuckooTrainSensor {
	public void stop(String id) throws Exception;

	public void start(String id, String valuePath,
			android.os.Bundle configuration) throws Exception;
}
