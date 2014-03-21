import java.util.Queue;
import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;

abstract class SonarSensorListener extends Thread {
	protected UltrasonicSensor sensor;
	Queue<ControlThread.Reflex> queue;
	
	static final int distanceThreshold = 12;
	
	public SonarSensorListener(SensorPort port, Queue<ControlThread.Reflex> queue) {
		this.queue = queue;
		sensor = new UltrasonicSensor(port);
	}
	
	public void run() {
		while (true) {
			if (sensor.getDistance() < distanceThreshold) {
				synchronized (queue) {
					reflex();
				}
			}
		}
	}
	
	abstract protected void reflex();
}