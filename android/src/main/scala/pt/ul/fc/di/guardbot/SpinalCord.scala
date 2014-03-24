package pt.ul.fc.di.guardbot

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import scala.collection.JavaConversions.asScalaSet
import akka.actor._
import android.bluetooth.{ BluetoothSocket, BluetoothAdapter }

object SpinalCord {
  case object AttachBody
  case object DetachBody
  case class Move(t: Double, r: Double)
  case object Shoot
}

/* This acts as a bridge between the brain and the body */
class SpinalCord extends Actor {
  import SpinalCord._

  lazy val mouth = context.actorSelection("../mouth")

  var socket: Option[BluetoothSocket] = None
  var dataIn: Option[DataInputStream] = None
  var dataOut: Option[DataOutputStream] = None

  var lastMove = System.currentTimeMillis
  var fired = false

  def receive = {
    case AttachBody ⇒
      BluetoothAdapter.getDefaultAdapter.getBondedDevices.find(_.getName == "NXT") match {
        case Some(d) ⇒
          val sock = d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
          sock.connect()
          mouth ! Mouth.Say("I can feel my body!")
          socket = Some(sock)
          dataOut = Some(new DataOutputStream(sock.getOutputStream))
          dataIn = Some(new DataInputStream(sock.getInputStream))
        case None ⇒
          mouth ! Mouth.Say("Could not connect to my body!")

      }
    case DetachBody ⇒
      dataOut.foreach(_.writeInt(2))
      dataOut.foreach(_.close())
      dataOut = None
      dataIn.foreach(_.close())
      dataIn = None
      socket.foreach(_.close())
      socket = None
    case Move(t, r) ⇒
      if (System.currentTimeMillis - lastMove > 200) {
        dataOut foreach { out ⇒
          //out.writeInt(0)
          //out.writeInt(0)
          //out.writeDouble(t)
          //out.writeDouble(r)
        }
        lastMove = System.currentTimeMillis
      }
    case Shoot ⇒
      if (!fired) {
        dataOut.foreach(_.writeInt(1))
        fired = true
      }
  }

  override def postStop() = {

  }
}
