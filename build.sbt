import play.Project._

name := """email-sender"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0", 
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1",
  "com.twilio.sdk" % "twilio-java-sdk" % "3.3.16"
)

playScalaSettings
