package ems.models

import play.api.libs.json.Json


sealed case class Signal(content: String)

object Signal {
  implicit val signalFormat = Json.format[Signal]
}

object Ping extends Signal("ping")
