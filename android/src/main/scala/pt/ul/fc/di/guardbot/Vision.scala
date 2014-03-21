package pt.ul.fc.di.guardbot

import android.view.SurfaceView
import akka.actor._
import android.hardware.Camera
import android.hardware.Camera.FaceDetectionListener
import macroid.AppContext

object Vision {
  case class OpenEyes(surface: Option[SurfaceView])
  case object CloseEyes
	case class Face(face: Camera.Face)
}

/* An actor that monitors the camera */
class Vision(implicit ctx: AppContext) extends Actor {
	import context._
	import Vision._

  var retina: Option[SurfaceView] = None
	var eye: Option[Camera] = None

  lazy val brain = actorSelection("../../brain")

	def receive = {
		case OpenEyes(surface) ⇒
      retina = surface
      eye = Some(Camera.open(1))
      (eye zip retina) foreach { case (e, r) ⇒
        val params = e.getParameters
        params.setZoom(params.getMaxZoom / 2)
        e.setParameters(params)
        e.setPreviewDisplay(r.getHolder)
        e.setDisplayOrientation(90)
        e.startPreview()
        e.setFaceDetectionListener(new FaceDetectionListener {
          def onFaceDetection(faces: Array[Camera.Face], camera: Camera) = if (faces.length > 0) {
            brain ! Face(faces(0))
          }
        })
        e.startFaceDetection()
      }

    case CloseEyes ⇒
      eye foreach { e ⇒
        e.stopFaceDetection()
        e.stopPreview()
        e.release()
      }
      eye = None
      retina = None
	}
}
