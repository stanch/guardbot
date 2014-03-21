import java.util.Queue;
import lejos.nxt.Motor;
import lejos.nxt.NXTRegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;

public class ControlThread extends Thread {
	public static class Reflex {
		Double travel;
		Double rotate;
		public Reflex(Double travel, Double rotate) {
			this.travel = travel;
			this.rotate = rotate;
		}
	}

	public static class BrainWave {
		Double linear;
		Double angular;
		boolean separate;
		public BrainWave(boolean separate, Double linear, Double angular) {
			this.separate = separate;
			this.linear = linear;
			this.angular = angular;
		}
	}
	
	public ControlThread(Queue<Integer> outputQueue) {
		this.outputQueue = outputQueue;
	}

	public volatile Queue<Reflex> reflexQueue = new Queue<Reflex>();
	public volatile Queue<BrainWave> brainWaveQueue = new Queue<BrainWave>();
	Queue<Integer> outputQueue;
	DifferentialPilot pilot = new DifferentialPilot(30, 120, Motor.A, Motor.B, true);
	NXTRegulatedMotor right = Motor.A;
	NXTRegulatedMotor left = Motor.B;
	boolean alive = true;

	@Override
	public void run() {
		pilot.setTravelSpeed(40);

		while (alive) {
			/* check for reflexes to execute */
			synchronized (reflexQueue) {
				if (!reflexQueue.empty()) {
					// notify the controller
					synchronized (outputQueue) {
						outputQueue.push(1);
					}
					
					// stop!
					pilot.stop();
					pilot.setTravelSpeed(40);

					// execute the reflex pattern
					while (!reflexQueue.empty()) {
						Reflex msg = (Reflex) reflexQueue.pop();
						if (msg.travel != null)
							pilot.travel(msg.travel);
						if (msg.rotate != null)
							pilot.rotate(msg.rotate);
					}

					// remove old messages sent by the brain
					synchronized (brainWaveQueue) {
						brainWaveQueue.clear();
					}
					
					// notify the controller
					synchronized (outputQueue) {
						outputQueue.push(0);
					}
				}
			}
			/* correct the movement as the brain tells */
			synchronized (brainWaveQueue) {
				if (!brainWaveQueue.empty()) {
					BrainWave msg = (BrainWave)brainWaveQueue.pop();
					if (!msg.separate) {
						if (msg.linear > 0) {
							pilot.setTravelSpeed(msg.linear);
							if (msg.angular != 0) {
								pilot.steer(msg.angular);
							} else {
								pilot.forward();
							}
						} else if (msg.linear < 0) {
							pilot.setTravelSpeed(-msg.linear);
							if (msg.angular != 0) {
								pilot.steerBackward(-msg.angular);
							} else {
								pilot.backward();
							}
						} else {
							if (msg.angular > 0) {
								pilot.setRotateSpeed(msg.angular);
								pilot.rotateLeft();
							} else {
								pilot.setRotateSpeed(-msg.angular);
								pilot.rotateRight();
							}
						}
					} else {
						if (msg.linear > 0) {
							left.forward();
							left.setSpeed(msg.linear.floatValue());
						} else {
							left.backward();
							left.setSpeed(-msg.linear.floatValue());
						}
						if (msg.angular > 0) {
							right.forward();
							right.setSpeed(msg.angular.floatValue());
						} else {
							right.backward();
							right.setSpeed(-msg.angular.floatValue());
						}
					}
				}
			}
		}
	}
}