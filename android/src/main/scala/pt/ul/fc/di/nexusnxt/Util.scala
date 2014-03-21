package pt.ul.fc.di.nexusnxt

import android.hardware.Camera
import akka.actor._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_imgproc._
import math._
import android.util.Log
import scala.util.continuations._
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import org.scaloid.common._
import android.app.Activity
import android.view.ViewTreeObserver
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.SurfaceView
import android.view.SurfaceHolder

object ContinuationUI {
	implicit class RichAlertDialogBuilder(d: AlertDialog.Builder) {
		def createShowModal(): Unit @cps[Unit] = shift { c: (Unit ⇒ Unit) ⇒
			d.setPositiveButton("OK", { (_: DialogInterface, _: Int) ⇒ c() }).create().show()
		}
	}
	
	def waitForSurface(s: SurfaceView) = shift { c: (Unit ⇒ Unit) ⇒
		s.getHolder.addCallback(new SurfaceHolder.Callback() {
			override def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
				s.getHolder.removeCallback(this)
				c()
			}
			override def surfaceCreated(holder: SurfaceHolder) {}
			override def surfaceDestroyed(holder: SurfaceHolder) {}
		})
	}
}

object Implicits {
	implicit def func2FaceDetectionListener(f: (Array[Camera.Face], Camera) ⇒ Any) = new Camera.FaceDetectionListener() {
		def onFaceDetection(faces: Array[Camera.Face], camera: Camera) = f(faces, camera)
	}
	
	implicit def func2PreviewCallback(f: (Array[Byte], Camera) ⇒ Any) = new Camera.PreviewCallback() {
		def onPreviewFrame(data: Array[Byte], camera: Camera) = f(data, camera)
	}
}

class Killer(victim: ActorRef, reward: ⇒ Any) extends Actor {
	context.watch(victim)
	victim ! PoisonPill
	def receive = { case Terminated(`victim`) ⇒ reward }
}

object IplListMatVector {
	implicit def iplList2matVector(l: List[IplImage]) = {
		val mat = new MatVector(l.length)
		for (i ← l.indices) mat.put(i, l(i))
		mat
	}
}

/* Camera and OpenCV utilities */
class FaceReader(resizeTo: Int)(implicit camera: Camera) {	
	lazy val size = camera.getParameters.getPreviewSize
	lazy val cameraImg = cvCreateImage(cvSize(size.width, size.height), IPL_DEPTH_8U, 4)
	lazy val scaledImg = cvCreateImage(cvSize(resizeTo, resizeTo), IPL_DEPTH_8U, 4)
	lazy val grayedImg = cvCreateImage(cvSize(resizeTo, resizeTo), IPL_DEPTH_8U, 1)
	lazy val tmp = new Array[Int](size.width * size.height)
	
	def scale(x: Int, l: Int) = l/2 + l*x/2000
	
	def readImage(data: Array[Byte], face: Camera.Face): IplImage = {
		// convert to IplImage
		yuv2rgb(tmp, data, size.width, size.height)
        cameraImg.getIntBuffer.put(tmp)
        
        // extract the face
        cvSetImageROI(cameraImg, cvRect(
			scale(face.rect.left, cameraImg.width),
			scale(face.rect.top, cameraImg.height),
			cameraImg.width*face.rect.width/2000,
			cameraImg.height*face.rect.height/2000
		))
		val s = cvGetSize(cameraImg)
		val cropped = cvCreateImage(s, IPL_DEPTH_8U, 4)
		val rotated = cvCreateImage(cvSize(s.height, s.width), IPL_DEPTH_8U, 4)
		cvCopy(cameraImg, cropped)
		cvResetImageROI(cameraImg)
		cvTranspose(cropped, rotated)
		cvFlip(rotated, null, 0)
		
		// scale
		cvResize(rotated, scaledImg)
		
		scaledImg
	}
	
	def looseColorIntoNew(img: IplImage): IplImage = {
		val gray = cvCreateImage(cvSize(resizeTo, resizeTo), IPL_DEPTH_8U, 1)
		cvCvtColor(img, gray, CV_BGR2GRAY)
		gray
	}
	
	def looseColor(img: IplImage): IplImage = {
		cvCvtColor(scaledImg, grayedImg, CV_BGR2GRAY)
		grayedImg
	}

	def yuv2rgb(rgb: Array[Int], yuv: Array[Byte], width: Int, height: Int) {
        val frameSize = width * height
        var (j, yp) = (0, 0)
        
        while (j < height) {
            var uvp = frameSize + (j >> 1) * width
            var (u, v) = (0, 0)
            var i = 0
            
            while (i < width) {
                val y = max((0xff & yuv(yp).toInt) - 16, 0)
                
                if ((i & 1) == 0) {
                    v = (0xff & yuv(uvp)) - 128
                    u = (0xff & yuv(uvp+1)) - 128
                    uvp += 2
                }
                
                val y1192 = 1192 * y
                val r = min(max(y1192 + 1634 * v, 0), 0x3ffff)
                val g = min(max(y1192 - 833 * v - 400 * u, 0), 0x3ffff)
                val b = min(max(y1192 + 2066 * u, 0), 0x3ffff)

            	rgb(yp) = 0xff000000 | ((b << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((r >> 10) & 0xff)
            	yp += 1
            	i += 1
            }
            j += 1
        }
    }
}