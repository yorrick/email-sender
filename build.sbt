import CoverallsPlugin.CoverallsKeys._


name := """email-sender"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

scalacOptions ++= Seq("-feature", "-deprecation")

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "rediscala" at "https://raw.github.com/etaty/rediscala-mvn/master/releases/"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0", 
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.6.0",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "com.github.nscala-time" %% "nscala-time" % "1.2.0",
  "fr.njin" %% "play2-rediscala" % "1.0.1" exclude("org.scala-stm", "scala-stm_2.10.0")
)


/// Scoverage plugin
instrumentSettings

ScoverageKeys.minimumCoverage := 70

ScoverageKeys.failOnMinimumCoverage := false

ScoverageKeys.highlighting := {
  if (scalaBinaryVersion.value == "2.10") false
  else false
}

ScoverageKeys.excludedPackages in ScoverageCompile := "<empty>;Reverse.*;"

CoverallsPlugin.coverallsSettings

coverallsToken := "fTcPVrhbSaPBVjGYlhRkGX8DqU8lQHrFf"

