import CoverallsPlugin.CoverallsKeys._


name := """email-sender"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0", 
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.6.0"
)

/// Scoverage plugin
instrumentSettings

ScoverageKeys.minimumCoverage := 70

ScoverageKeys.failOnMinimumCoverage := false

ScoverageKeys.highlighting := {
  if (scalaBinaryVersion.value == "2.10") false
  else false
}

ScoverageKeys.excludedPackages in ScoverageCompile := "<empty>;Reverse.*;.*AuthService.*;models\.data\..*"

CoverallsPlugin.coverallsSettings

coverallsToken := "fTcPVrhbSaPBVjGYlhRkGX8DqU8lQHrFf"
