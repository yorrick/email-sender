package ems.views.utils

import play.api.data.Form
import play.api.mvc.Call


case class FormInfo[T](form: Form[T], postAction: Call, messages: Map[String, String] = Map())


