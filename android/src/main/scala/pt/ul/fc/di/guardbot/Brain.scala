package pt.ul.fc.di.guardbot

import android.view.SurfaceView
import scala.concurrent.duration._
import akka.actor._
import scala.math.abs
import scala.util.Random
import macroid.AppContext

object Brain {
  sealed trait State
  case object Wandering extends State
  case object Chasing extends State
  case object Sleeping extends State

  case class WakeUp(retina: Option[SurfaceView])
  case object Sleep
}

/* A state machine responsible for the robot’s behavior */
class Brain(implicit ctx: AppContext) extends Actor with FSM[Brain.State, Unit] {
  import context._
  import Brain._

  lazy val vision = actorOf(Props(new Vision))
  lazy val spine = actorOf(Props(new SpinalCord))
  lazy val mouth = actorOf(Props(new Mouth))

  var lastWord = System.currentTimeMillis
  def say(word: String) = {
    if (System.currentTimeMillis - lastWord > 10000) {
      mouth ! Mouth.Say(word)
      lastWord = System.currentTimeMillis
    }
  }

  def catchPhrase = List(
    "Looking for someone...",
    "Come on, where are you guys?",
    "Just show me your face!"
  )(Random.nextInt(3))

  startWith(Sleeping, Unit)

  when(Sleeping) {
    case Event(WakeUp(retina), _) ⇒
      mouth ! Mouth.Say("I am alive!")
      spine ! SpinalCord.AttachBody
      vision ! Vision.OpenEyes(retina)
      spine ! SpinalCord.Shoot
      goto(Wandering)
    case _ ⇒ stay()
  }

  /* Wandering around and looking for people */
  when(Wandering, stateTimeout = 1 second) {
    case Event(Vision.Face(_), _) ⇒
      goto(Chasing)
    case Event(Sleep, _) ⇒
      vision ! Vision.CloseEyes
      spine ! SpinalCord.DetachBody
      goto(Sleeping)
    case Event(FSM.StateTimeout, _) ⇒
      spine ! SpinalCord.Move(0, 10)
      say(catchPhrase)
      stay()
  }

  /* Chasing the face */
  when(Chasing, stateTimeout = 3 seconds) {
    case Event(Vision.Face(face), _) ⇒
      if (face.rect.exactCenterX > 600 && abs(face.rect.exactCenterY) < 200) {
        // shoot
        mouth ! Mouth.Say("Fire!!!")
        spine ! SpinalCord.Shoot
        goto(Wandering)
      } else {
        // calculate movement speed
        spine ! SpinalCord.Move(
          -70 * (face.rect.exactCenterX - 700) / 600 + 10,
          70 * face.rect.exactCenterY / 1000
        )
        stay()
      }
    case Event(Sleep, _) ⇒
      vision ! Vision.CloseEyes
      spine ! SpinalCord.DetachBody
      goto(Sleeping)
    case Event(FSM.StateTimeout, _) ⇒
      goto(Wandering)
  }

  initialize()
}
