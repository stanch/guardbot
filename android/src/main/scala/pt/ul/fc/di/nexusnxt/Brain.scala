package pt.ul.fc.di.nexusnxt

import android.view.SurfaceView
import scala.concurrent.duration._
import akka.actor._
import android.content.Context
import org.scaloid.common._
import scala.math.{signum, abs}
import android.util.Log
import scala.util.Random
import android.speech.tts.TextToSpeech

object Brain {
	sealed trait State
	case object Wandering extends State
	case object Chasing extends State
}

/* A state machine responsible for the robot’s behavior */
class Brain(retina: SurfaceView)(implicit ctx: Context) extends Actor with FSM[Brain.State, Unit] {
	import context._
	import Brain._
	import Vision._
	import Mouth._

	val vision = actorOf(Props(new Vision(retina)))
	val spine = actorOf(Props[SpinalCord])
	val mouth = actorOf(Props(new Mouth))
	
	var lastWord = System.currentTimeMillis
	def say(word: String) = {
		if (System.currentTimeMillis - lastWord > 10000) {
			mouth ! Say(word)
			lastWord = System.currentTimeMillis
		}
	}

	startWith(Wandering, Unit)

	/* Wandering around and looking for people */
	when(Wandering, stateTimeout = 1 second) {
		case Event(Face(_, _), _) ⇒
			goto(Chasing)
		case Event(FSM.StateTimeout, _) ⇒
			spine ! SpinalCord.Move(0, 10)
			say(List(
				"""Looking for someone...""",
				"""Come on, where are you guys?""",
				"""Just show me your face!"""
			)(Random.nextInt(3)))
			stay
	}

	/* Chasing the face */
	when(Chasing, stateTimeout = 3 seconds) {
		case Event(Face(face, Some("nick")), _) ⇒
			// greet the creator
			say("Oh, hi, Nick!")
			stay
		case Event(Face(face, name), _) ⇒
			// sometimes say a random catch-phrase
			name foreach { n ⇒
				say(List(
					s"""Gonna shoot you, $n!""",
					s"""$n, are you feeling lucky today?""",
					s"""Beware, $n, I am coming!"""
				)(Random.nextInt(3)))
			}
			if (face.rect.exactCenterX > 600 && abs(face.rect.exactCenterY) < 200) {
				// shoot
				mouth ! Say("Fire!!!")
				spine ! SpinalCord.Shoot
				goto(Wandering)
			} else {
				// calculate movement speed
				spine ! SpinalCord.Move(
					-70 * (face.rect.exactCenterX - 700) / 600 + 10,
					70 * face.rect.exactCenterY / 1000
				)
				stay
			}
		case Event(FSM.StateTimeout, _) ⇒
			goto(Wandering)
	}

	initialize
}