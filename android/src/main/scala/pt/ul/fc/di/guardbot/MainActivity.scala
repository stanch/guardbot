package pt.ul.fc.di.guardbot

import akka.actor._
import android.app.Activity
import android.os.Bundle
import android.view.{ Gravity, SurfaceView }
import android.widget.{ FrameLayout, Button, LinearLayout }
import macroid.Contexts
import macroid.FullDsl._
import macroid.util.Ui
import macroid.contrib.Layouts.VerticalLinearLayout
import com.typesafe.config.ConfigFactory

class MainActivity extends Activity with Contexts[Activity] {
  lazy val classLoader = getApplication.getClassLoader
  lazy val actorSystem = ActorSystem("ActorSystem", ConfigFactory.load(classLoader), classLoader)
  lazy val brain = actorSystem.actorOf(Props(new Brain), name = "brain")

  var surface = slot[SurfaceView]

  lazy val stoppedView = l[FrameLayout](
    w[Button] <~ text("Start") <~ On.click(wakeUp) <~ lp[FrameLayout](100 dp, 50 dp, Gravity.CENTER)
  )

  lazy val startedView = l[VerticalLinearLayout](
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
    brain ! Brain.Sleep
    setContentView(getUi(stoppedView))
  }
}
