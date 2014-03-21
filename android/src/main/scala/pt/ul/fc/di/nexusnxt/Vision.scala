package pt.ul.fc.di.nexusnxt

import android.view.SurfaceView
import scala.concurrent.duration._
import akka.actor._
import android.content.Context
import android.hardware.Camera
import Implicits._

object Vision {
	case class Face(face: Camera.Face, name: Option[String])
}

/* An actor that monitors the camera */
class Vision(retina: SurfaceView)(implicit ctx: Context) extends Actor {
	import context._
	import Vision._

	implicit var eye: Camera = _
	val memory = actorOf(Props(new Memory))
	
	var person: Option[String] = None
	var lastRecall = System.currentTimeMillis

	override def preStart {
		eye = Camera.open(1)
		val params = eye.getParameters
		params.setZoom(params.getMaxZoom / 2)
		eye.setParameters(params)
		eye.setPreviewDisplay(retina.getHolder)
		eye.setDisplayOrientation(90)
		eye.startPreview()
		eye.setFaceDetectionListener { (faces: Array[Camera.Face], _: Camera) ⇒
			if (faces.length > 0) synchronized {
				// try to recognize the person
				if (eye != null && System.currentTimeMillis-lastRecall > 2000) {
					eye.setOneShotPreviewCallback { (data: Array[Byte], _: Camera) ⇒
						memory ! Memory.WhoIs(data, faces(0))
					}
					lastRecall = System.currentTimeMillis
				}
				// send the face to the brain
				actorFor("../../brain") ! Face(faces(0), person)
			}
		}
		eye.startFaceDetection()
	}

	override def postStop {
		if (eye != null) {
			eye.stopFaceDetection()
			eye.stopPreview()
			synchronized { eye.release(); eye = null }
		}
	}

	def receive = {
		case Memory.IKnow(name) ⇒
			person = name
		case _ ⇒ ;
	}
}