package pt.ul.fc.di.nexusnxt

import android.speech.tts.TextToSpeech
import scala.concurrent.Promise
import android.content.Context
import akka.actor.Actor
import android.util.Log
import java.util.Locale
import org.scaloid.common._


object Mouth {
	case class Say(text: String)
}

/* An actor that speaks */
class Mouth(implicit ctx: Context) extends Actor {
	import context._
	import Mouth._

	var lipsReady = false
	var lips: TextToSpeech = _
	lips = new TextToSpeech(ctx, new TextToSpeech.OnInitListener {
		override def onInit(status: Int) = if (status == TextToSpeech.SUCCESS) {
			lips.setLanguage(Locale.US)
			lips.speak("I am ready for great adventures", TextToSpeech.QUEUE_FLUSH, null)
			lipsReady = true
		}
	})
	
	override def receive = {
		case Mouth.Say(text) ⇒
			if (lipsReady) {
				lips.speak(text, TextToSpeech.QUEUE_FLUSH, null)
			}
	}
}