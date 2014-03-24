name := "guardbot-lejos"

javacOptions ++= Seq("-bootclasspath", "lib\\classes.jar")

val upload = TaskKey[Unit]("upload", "Upload code to the NXT")

upload := {
  Process(List("C:\\Program Files (x86)\\leJOS NXJ\\bin\\nxj.bat", "Main"), cwd = (classDirectory in Compile).value).!
}
