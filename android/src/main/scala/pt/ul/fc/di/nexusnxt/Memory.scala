package pt.ul.fc.di.nexusnxt

import com.googlecode.javacv.cpp.opencv_contrib.createFisherFaceRecognizer
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import akka.actor.Actor
import akka.actor.actorRef2Scala
import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.hardware.Camera.Face
import android.util.Log

object Memory {
	case class WhoIs(data: Array[Byte], face: Face)
	case class IKnow(name: Option[String])
}

/* An actor that accepts recognition requests and replies with reminiscences */
class Memory(implicit ctx: Context, cam: Camera) extends Actor {
	import Memory._
	
	lazy val recognizer = createFisherFaceRecognizer
	lazy val namesLabels = ctx.getExternalCacheDir.listFiles.filter(_.isDirectory).map(_.getName).zipWithIndex.toMap.map(_.swap)
	lazy val dsp = new FaceReader(200)
	
	override def preStart = {
		recognizer.load(ctx.getExternalCacheDir.getAbsolutePath + "/faces.yml")
	}
	
	def receive = {
		case WhoIs(data, face) ⇒
			val img = dsp.looseColor(dsp.readImage(data, face))
			val who = recognizer.predict(img)
			sender ! IKnow(if (who>=0) Some(namesLabels(who)) else None)
	}
}