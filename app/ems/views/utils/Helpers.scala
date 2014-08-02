package ems.views.utils


import ems.views.html.utils.emsFieldConstructorTemplate
import views.html.helper.FieldConstructor


object Helpers {
  implicit val emsFields = FieldConstructor(emsFieldConstructorTemplate.f)


  val warning = "warning"
  val success = "success"
  val alertsTypes = Map(
    warning -> "alert-danger",
    success -> "alert-success"
  )
}