package pt.ul.fc.di.guardbot

import android.speech.tts.TextToSpeech
import akka.actor.Actor
import java.util.Locale
import macroid.AppContext

object Mouth {
  case class Say(text: String)
}

/* An actor that speaks */
class Mouth(implicit ctx: AppContext) extends Actor {
  var lips: Option[TextToSpeech] = None

  val tts: TextToSpeech = new TextToSpeech(ctx.get, new TextToSpeech.OnInitListener {
    override def onInit(status: Int) = if (status == TextToSpeech.SUCCESS) {
      tts.setLanguage(Locale.US)
      tts.speak("I am ready for great adventures", TextToSpeech.QUEUE_FLUSH, null)
      lips = Some(tts)
    }
  })

  override def receive = {
    case Mouth.Say(text) ⇒ lips.foreach(_.speak(text, TextToSpeech.QUEUE_FLUSH, null))
  }
}
