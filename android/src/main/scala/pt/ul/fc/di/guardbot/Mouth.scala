package pt.ul.fc.di.guardbot

import android.speech.tts.TextToSpeech
import akka.actor.{ Stash, Actor }
import java.util.Locale
import macroid.AppContext

object Mouth {
  case class Say(text: String)
}

/* An actor that speaks */
class Mouth(implicit ctx: AppContext) extends Actor with Stash {
  import Mouth._

  var lips: Option[TextToSpeech] = None

  val tts: TextToSpeech = new TextToSpeech(ctx.get, new TextToSpeech.OnInitListener {
    override def onInit(status: Int) = if (status == TextToSpeech.SUCCESS) {
      tts.setLanguage(Locale.US)
      tts.speak("I am ready for great adventures", TextToSpeech.QUEUE_FLUSH, null)
      lips = Some(tts)
      unstashAll()
      context.become(teethBrushed)
    }
  })

  def teethBrushed: Receive = {
    case Say(text) ⇒ lips.foreach(_.speak(text, TextToSpeech.QUEUE_FLUSH, null))
  }

  override def receive = {
    case msg ⇒ stash()
  }
}
