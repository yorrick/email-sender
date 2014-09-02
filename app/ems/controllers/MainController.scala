package ems.controllers

import ems.controllers.utils.{ContextAction, ContextRequest}
import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.SecureSocial
import ems.models.User
import scala.concurrent.ExecutionContext


class MainController(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject[RuntimeEnvironment[User]]
  implicit val executionContext = inject[ExecutionContext]

  def index = ContextAction("welcome", "footer") {
    UserAwareAction { r: RequestWithUser[_] => r match {
      case RequestWithUser(user, authenticator, ContextRequest(ctx, originalRequest)) =>

        implicit val u = user
        implicit val c = ctx

        Ok(ems.views.html.index())

      }
    }
  }


}
