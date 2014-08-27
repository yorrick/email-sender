package ems.views.utils


object Helpers {

  case class Alert(code: String, cssClass: String, iconName: String)
  object warning extends Alert("warning", "alert-danger", "glyphicon-warning-sign")
  object success extends Alert("success", "alert-success", "glyphicon-ok")

  val alerts = List(warning, success)
}