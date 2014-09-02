package ems.controllers.utils

import play.api.mvc.{WrappedRequest, Request}


case class ContextRequest[A](val context: Context, request: Request[A]) extends WrappedRequest[A](request)
