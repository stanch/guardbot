import java.util.Queue;

import lejos.nxt.SensorPort;

public class Main {
	public static void main(String[] args) {
		/* create a controller */
		Queue<Integer> outputQueue = new Queue<Integer>();
		ControlThread control = new ControlThread(outputQueue);

		/* setup event handlers */
		//new RearTouchSensorListener(SensorPort.S1, control.reflexQueue).start();
		//new FrontTouchSensorListener(SensorPort.S4, control.reflexQueue).start();
		new RearSonarSensorListener(SensorPort.S2, control.reflexQueue).start();
		new FrontSonarSensorListener(SensorPort.S3, control.reflexQueue).start();

		/* start working! */
		control.start();
		new SpinalCordThread(control.brainWaveQueue, outputQueue).start();
	}
}
