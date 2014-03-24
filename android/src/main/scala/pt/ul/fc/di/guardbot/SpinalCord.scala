package pt.ul.fc.di.guardbot

import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.UUID
import scala.collection.JavaConversions.asScalaSet
import scala.concurrent.duration._
import akka.actor._
import android.bluetooth.{ BluetoothSocket, BluetoothAdapter }

object SpinalCord {
  case object AttachBody
  case object DetachBody
  case class Move(t: Double, r: Double)
  case object Shoot
  case object ReflexCheck
}

/* This acts as a bridge between the brain and the body */
class SpinalCord extends Actor {
  import SpinalCord._
  import context.dispatcher

  lazy val mouth = context.actorSelection("../mouth")

  var socket: Option[BluetoothSocket] = None
  var dataIn: Option[DataInputStream] = None
  var dataOut: Option[DataOutputStream] = None

  var lastMove = System.currentTimeMillis
  var lastShot = System.currentTimeMillis
  var reflexTimer: Option[Cancellable] = None

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
          reflexTimer = Some(context.system.scheduler.schedule(500 millis, 500 millis, self, ReflexCheck))
        case None ⇒
          mouth ! Mouth.Say("Could not find my body!")
      }
    case DetachBody ⇒
      reflexTimer.foreach(_.cancel())
      reflexTimer = None
      dataOut.foreach(_.writeInt(2))
      dataOut.foreach(_.close())
      dataOut = None
      dataIn.foreach(_.close())
      dataIn = None
      socket.foreach(_.close())
      socket = None
    case ReflexCheck ⇒
      dataIn.filter(_.available > 0) foreach { in ⇒
        if (in.readInt() == 1) mouth ! Mouth.Say("Oops!")
      }
    case Move(t, r) ⇒
      dataOut.filter(_ ⇒ System.currentTimeMillis - lastMove > 200) foreach { out ⇒
        out.writeInt(0)
        out.writeInt(0)
        out.writeDouble(t)
        out.writeDouble(r)
        lastMove = System.currentTimeMillis
      }
    case Shoot ⇒
      dataOut.filter(_ ⇒ System.currentTimeMillis - lastShot > 4000) foreach { out ⇒
        out.writeInt(1)
        lastShot = System.currentTimeMillis()
      }
  }
}
