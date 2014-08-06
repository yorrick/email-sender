package ems.views.utils


import ems.views.html.utils.emsFieldConstructorTemplate
import views.html.helper.FieldConstructor


object Helpers {
  implicit val emsFields = FieldConstructor(emsFieldConstructorTemplate.f)

  case class Alert(code: String, cssClass: String, iconName: String)
  object warning extends Alert("warning", "alert-danger", "glyphicon-warning-sign")
  object success extends Alert("success", "alert-success", "glyphicon-ok")

  val alerts = List(warning, success)
}