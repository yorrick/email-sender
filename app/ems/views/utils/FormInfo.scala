package ems.views.utils

import play.api.data.Form
import play.api.libs.json.JsValue
import play.api.mvc.Call


case class FormInfo[T](form: Form[T], postAction: Call, frontendValidationParams: JsValue)
