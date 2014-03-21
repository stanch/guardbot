package pt.ul.fc.di.nexusnxt

import org.scaloid.common._
import scala.util.continuations._
import akka.actor._
import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Button
import android.widget.LinearLayout
import com.googlecode.javacv.cpp.opencv_contrib._
import com.googlecode.javacv.cpp.opencv_core._
import com.googlecode.javacv.cpp.opencv_highgui._
import com.googlecode.javacv.cpp.opencv_imgproc._
import java.io.File
import android.hardware.Camera
import Implicits._
import IplListMatVector._
import android.util.Log
import scala.collection.JavaConversions._
import android.widget.TextView
import android.app.AlertDialog
import android.widget.EditText
import android.content.DialogInterface
import android.content.Context
import ContinuationUI._
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.widget.ScrollView

/* A testbed to train the robot to recognize faces */
class TrainingActivity extends Activity with SActivity {
	lazy val surf = new SurfaceView(ctx)
	lazy val faceRecognizer = createFisherFaceRecognizer
	
	lazy val boothLayout = surf
	
	def mainLayout: LinearLayout = new LinearLayout(ctx) {
		setOrientation(LinearLayout.VERTICAL)
		this += new Button(ctx) {
			setText("Train")
			setOnClickListener(train)
		}
		this += new Button(ctx) {
			setText("Test")
			setOnClickListener(test)
		}
		this += new Button(ctx) {
			setText("Add new")
			setOnClickListener(photoshoot)
		}
		this += new ScrollView(ctx) {
			this += new LinearLayout(ctx) {
				setOrientation(LinearLayout.VERTICAL)
				getExternalCacheDir.listFiles.filter(_.isDirectory) map { d ⇒
					this += new TextView(ctx) {
						setText(d.getName)
					}
					this += new LinearLayout(ctx) {
						setOrientation(LinearLayout.HORIZONTAL)
						d.listFiles map { f ⇒
							this += new ImageView(ctx) {
								setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath))
							}
						}
					}
				}
			}
		}
	}

	override def onCreate(savedInstanceState: Bundle) {
		super.onCreate(savedInstanceState)
		setContentView(mainLayout)
	}
	
	implicit var eye: Camera = _
	lazy val dsp = new FaceReader(200)
	
	/* Capture n faces and call the continuation */
	def captureFaces(n: Int) = shift { c: (List[IplImage] ⇒ Unit) ⇒
		var images = List[IplImage]()
		var last = System.currentTimeMillis
		eye = Camera.open(1)
		eye.setPreviewDisplay(surf.getHolder)
		eye.setDisplayOrientation(90)
		eye.startPreview()
		eye.setFaceDetectionListener { (faces: Array[Camera.Face], _: Camera) ⇒
			if (faces.length > 0) synchronized {
				if (eye != null && System.currentTimeMillis-last > 500) {
					eye.setOneShotPreviewCallback { (data: Array[Byte], _: Camera) ⇒
						images ::= dsp.readImage(data, faces(0))
						synchronized { last = System.currentTimeMillis }
						if (images.length == n) {
							eye.stopFaceDetection()
							eye.stopPreview()
							c(images)
							synchronized { eye.release(); eye = null }
						}
					}
				}
			}
		}
		eye.startFaceDetection()
	}

	/* Add a new person with a set of faces */
	def photoshoot(): Unit = reset {
		setContentView(boothLayout)
		waitForSurface(surf)
		
		// ask for a name
		val name = new EditText(ctx)
		new AlertDialog.Builder(ctx).setView(name).createShowModal()
		
		// prepare a directory
		val dir = new File(getExternalCacheDir.getAbsolutePath + "/" + name.getText.toString)
		dir.mkdirs()
		dir.listFiles.foreach(_.delete())
		
		// save the faces
		captureFaces(10) foreach { img ⇒
			cvSaveImage(File.createTempFile("face", ".jpg", dir).getAbsolutePath, img)
		}
		
		setContentView(mainLayout)
	}
	
	/* Test the algorithm */
	def test() = reset {
		setContentView(boothLayout)
		waitForSurface(surf)
		
		// load data
		faceRecognizer.load(getExternalCacheDir.getAbsolutePath + "/faces.yml")
		val namesLabels = getExternalCacheDir.listFiles.filter(_.isDirectory).map(_.getName).zipWithIndex.toMap.map(_.swap)
		
		// take a picture
		val who = captureFaces(10).map(img ⇒ faceRecognizer.predict(dsp.looseColor(img))).groupBy(identity).maxBy(_._2.size)._1
		toast(if (who >= 0) namesLabels(who) else "Not sure")
		
		setContentView(mainLayout)
	}

	/* Train the algorithm */
	def train() {
		val namesLabels = getExternalCacheDir.listFiles.filter(_.isDirectory).map(_.getName).zipWithIndex.toMap
		val (faces, labels) = (namesLabels flatMap { case (name, lab) ⇒
			new File(getExternalCacheDir.getAbsolutePath + "/" + name).listFiles map { f ⇒
				dsp.looseColorIntoNew(cvLoadImage(f.getAbsolutePath)) -> lab
			}
		}).unzip
		
		faceRecognizer.train(faces.toList, labels.toArray)
		faceRecognizer.set("threshold", 5)
		faceRecognizer.save(getExternalCacheDir.getAbsolutePath + "/faces.yml")
	}
}