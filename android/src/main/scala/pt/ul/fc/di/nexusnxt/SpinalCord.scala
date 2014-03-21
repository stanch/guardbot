package pt.ul.fc.di.nexusnxt

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import scala.collection.JavaConversions.asScalaSet
import akka.actor._
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import scala.concurrent.Future
import scala.concurrent.duration._

object SpinalCord {
	case class Move(t: Double, r: Double)
	case object Shoot
}

/* This acts as a bridge between the brain and the body */
class SpinalCord extends Actor {
	import context._
	import SpinalCord._

	lazy val (nxtSocket, dataOut, dataIn) = {
		// try to connect to the body
		BluetoothAdapter.getDefaultAdapter().getBondedDevices() find { _.getName() == "R3D3" } match {
			case Some(d) ⇒
				val sock = d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
				sock.connect()
				(sock, new DataOutputStream(sock.getOutputStream()), new DataInputStream(sock.getInputStream()))
			case None ⇒ throw new IOException("Could not find my body!")
		}
	}

	var lastMove = System.currentTimeMillis
	var fired = false

	def receive = {
		case Move(t, r) ⇒
			if (System.currentTimeMillis - lastMove > 200) {
				dataOut.writeInt(0)
				dataOut.writeInt(0)
				dataOut.writeDouble(t)
				dataOut.writeDouble(r)
				lastMove = System.currentTimeMillis
			}
		case Shoot ⇒
			if (!fired) {
				dataOut.writeInt(1)
				fired = true
			}
	}

	override def postStop {
		dataOut.writeInt(2)
		dataOut.close()
		dataIn.close()
		nxtSocket.close()
	}
}