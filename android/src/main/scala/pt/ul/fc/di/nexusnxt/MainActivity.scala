package pt.ul.fc.di.nexusnxt

import akka.actor._
import android.app.Activity
import android.os.Bundle
import android.view.SurfaceView
import android.widget.Button
import android.widget.LinearLayout
import macroid.Contexts
import macroid.FullDsl._
import macroid.util.Ui
import macroid.contrib.Layouts.VerticalLinearLayout

class MainActivity extends Activity with Contexts[Activity] {
	lazy val actorSystem = ActorSystem("ActorSystem")
  lazy val brain = actorSystem.actorOf(Props(new Brain), name="brain")

	var surface = slot[SurfaceView]

  val stoppedView = l[LinearLayout](
    w[Button] <~ text("Start") <~ On.click(wakeUp)
  )

  val startedView = l[VerticalLinearLayout](
    w[Button] <~ text("Stop") <~ On.click(sleep),
    w[SurfaceView] <~ wire(surface)
  )
	
	override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(getUi(stoppedView))
  }
	
	lazy val wakeUp: Ui[Unit] = Ui {
		setContentView(getUi(startedView))
		brain ! Brain.WakeUp(surface)
	}
	
	lazy val sleep: Ui[Unit] = Ui {
    setContentView(getUi(stoppedView))
    brain ! Brain.Sleep
	}
}
