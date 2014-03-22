import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;

class SpinalCordThread extends Thread {
	Queue<ControlThread.BrainWave> queue;
	public volatile Queue<Integer> outputQueue;
    BTConnection connection;
	DataInputStream dataIn;
	DataOutputStream dataOut;
	
	enum Command {
		MOVE, SHOOT, DIE
	}

	public SpinalCordThread(Queue<ControlThread.BrainWave> queue, Queue<Integer> outputQueue) {
		this.queue = queue;
		this.outputQueue = outputQueue;
	}

	public void connect() {
		LCD.clear();
		LCD.drawString("Waiting", 0, 0);
		connection = Bluetooth.waitForConnection(0, NXTConnection.RAW);
		LCD.clear();
		LCD.drawString("Connected", 0, 0);
		dataIn = connection.openDataInputStream();
		dataOut = connection.openDataOutputStream();
		if (dataIn != null && dataOut != null) {
			Sound.buzz();
		}
	}

	@Override
	public void run() {
		connect();
		while (true) {
			try {
				int code = dataIn.readInt();
				Command command = Command.values()[code];
				switch (command) {
					case MOVE: {
						queue.push(new ControlThread.BrainWave(dataIn.readInt()==1, dataIn.readDouble(), dataIn.readDouble()));
						break;
					} case SHOOT: {
						Motor.C.rotate(-20); // rotates to shoot the gun
						Motor.C.rotate(20); // goes back to charge position
						break;
					} case DIE: {
						Sound.buzz();
						Sound.buzz();
						System.exit(0);
					}
				}
				
				synchronized (outputQueue) {
					if (!outputQueue.empty()) {
						dataOut.writeInt(((Integer)outputQueue.pop()).intValue());
						dataOut.flush();
					}
				}
			} catch (IOException e) {
				System.out.println("Read exception " + e);
			}
		}
	}
}