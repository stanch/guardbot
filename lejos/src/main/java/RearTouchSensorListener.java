import java.util.Queue;
import lejos.nxt.SensorPort;

public class RearTouchSensorListener extends TouchSensorListener {
	public RearTouchSensorListener(SensorPort port, Queue<ControlThread.Reflex> queue) {
		super(port, queue);
	}
	
	@Override
	protected void reflex() {
		queue.push(new ControlThread.Reflex(10.0,15.0));
	}
}
