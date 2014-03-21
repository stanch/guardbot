package pt.ul.fc.di.nexusnxt

import org.scaloid.common._
import akka.actor._
import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Button
import android.widget.LinearLayout
import android.view.View
import android.speech.tts.TextToSpeech
import java.util.Locale

class MainActivity extends Activity with SActivity {
	lazy val actorSystem = ActorSystem("ActorSystem")
	lazy val surface = new SurfaceView(ctx)
	
	lazy val stoppedView = new LinearLayout(ctx) {
    	this += new Button(ctx) {
    		setText("Start")
    		setOnClickListener(live)
    	}
    	this += new Button(ctx) {
    		setText("Learn faces")
    		setOnClickListener(startActivity(SIntent[TrainingActivity]))
    	}
    }
	
	lazy val startedView = new LinearLayout(ctx) {
		setOrientation(LinearLayout.VERTICAL)
		this += new Button(ctx) {
			setText("Stop")
			setOnClickListener(die)
		}
		this += surface
	}
	
	override def onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(stoppedView)
    }
	
	def live() {
		setContentView(startedView)
		actorSystem.actorOf(Props(new Brain(surface)), name="brain")
	}
	
	def die() {
		val brain = actorSystem.actorFor("/user/brain")
		val p = spinnerDialog("", "Dying...")
		actorSystem.actorOf(Props(new Killer(brain, runOnUiThread {
			p.cancel()
			setContentView(stoppedView)
		})))
	}
}