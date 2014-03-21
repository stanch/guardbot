import android.Keys._

android.Plugin.androidBuild

platformTarget in Android := "android-18"

name := "guardbot-android"

scalaVersion := "2.10.3"

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "jcenter" at "http://jcenter.bintray.com"
)

scalacOptions in (Compile, compile) ++= Seq(
  "-P:wartremover:cp:" + (dependencyClasspath in Compile).value.files.map(_.toURL.toString).find(_.contains("org.macroid/macroid_")).get,
  "-P:wartremover:traverser:macroid.warts.Ui"
)

libraryDependencies ++= Seq(
  "org.macroid" %% "macroid" % "2.0.0-20140313",
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  compilerPlugin("org.brianmckenna" %% "wartremover" % "0.7")
)

proguardScala in Android := true

proguardOptions in Android ++= Seq(
  "-ignorewarnings",
  "-keep class scala.Dynamic",
  "-keep class akka.actor.LightArrayRevolverScheduler { *; }",
  "-keep class akka.actor.LocalActorRefProvider { *; }",
  "-keep class akka.actor.CreatorFunctionConsumer { *; }",
  "-keep class akka.actor.TypedCreatorFunctionConsumer { *; }",
  "-keep class akka.dispatch.BoundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.UnboundedDequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.dispatch.DequeBasedMessageQueueSemantics { *; }",
  "-keep class akka.actor.LocalActorRefProvider$Guardian { *; }",
  "-keep class akka.actor.LocalActorRefProvider$SystemGuardian { *; }",
  "-keep class akka.dispatch.UnboundedMailbox { *; }",
  "-keep class akka.actor.DefaultSupervisorStrategy { *; }",
  "-keep class org.macroid.akkafragments.AkkaAndroidLogger { *; }",
  "-keep class akka.event.Logging$LogExt { *; }"
)
