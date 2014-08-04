import CoverallsPlugin.CoverallsKeys._

name := """email-sender"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

scalacOptions ++= Seq("-feature", "-deprecation")

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

//resolvers += "rediscala" at "https://raw.github.com/etaty/rediscala-mvn/master/releases/"
resolvers += "play2-rediscala" at "http://dl.bintray.com/yorrick/maven"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "font-awesome" % "4.1.0",
  "org.webjars" % "bootstrap" % "3.1.1",
  "org.webjars" % "jquery" % "2.1.1",
  "com.newrelic.agent.java" % "newrelic-agent" % "3.6.0",
  // TODO do not depend on SNAPSHOT!
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.akka23-SNAPSHOT",
  "com.github.nscala-time" %% "nscala-time" % "1.2.0",
  "fr.njin" %% "play2-rediscala" % "2.3.0.0" exclude("org.scala-stm", "scala-stm_2.10.0"),
  // TODO do not depend on SNAPSHOT!
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3" % "test"
)

libraryDependencies += ws

libraryDependencies += cache


// Scoverage plugin
instrumentSettings

ScoverageKeys.minimumCoverage := 70

ScoverageKeys.failOnMinimumCoverage := false

ScoverageKeys.highlighting := {
  if (scalaBinaryVersion.value == "2.10") false
  else false
}

ScoverageKeys.excludedPackages in ScoverageCompile := "<empty>;Reverse.*;.*.template.scala"

CoverallsPlugin.coverallsSettings

coverallsToken := "fTcPVrhbSaPBVjGYlhRkGX8DqU8lQHrFf"

net.virtualvoid.sbt.graph.Plugin.graphSettings

scalacOptions ++= Seq(
  "-feature", // Shows warnings in detail in the stdout
  "-language:reflectiveCalls"
)