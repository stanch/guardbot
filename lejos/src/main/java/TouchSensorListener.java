import java.util.Queue;
import lejos.nxt.SensorPort;
import lejos.nxt.SensorPortListener;
import lejos.nxt.TouchSensor;

abstract public class TouchSensorListener implements SensorPortListener {
	protected SensorPort port;
	protected TouchSensor sensor;
	protected Queue<ControlThread.Reflex> queue;

	// the sensor produces multiple messages, so
	// we do a time threshold filtering
	long lastPressed = System.currentTimeMillis();
	static final long timeThreshold = 500;
	
	public TouchSensorListener(SensorPort port, Queue<ControlThread.Reflex> queue) {
		this.port = port;
		this.queue = queue;
		sensor = new TouchSensor(port);
	}
	
	public void start() {
		port.addSensorPortListener(this);
	}
	
	public void stateChanged(SensorPort port, int oldValue, int newValue) {
		if (sensor.isPressed() && System.currentTimeMillis()-lastPressed>timeThreshold) {
			// produce the reflex
			synchronized (queue) {
				reflex();
			}
			// remember the timestamp
			lastPressed = System.currentTimeMillis();
		}
	}
	
	protected abstract void reflex();
}
