package ems.controllers.utils

import ems.models.User
import play.api.mvc.{Action, AnyContent, Request, Result}
import scaldi.Injector
import securesocial.core.SecureSocial
import scala.concurrent.Future


/**
 * Contains utilities to easily declare controller methods that use authentication and context
 */
trait ControllerUtils extends SecureSocial[User] {

  implicit val injector: Injector

  /**
   * Type for the callback for secured actions
   */
  type securedControllerCallback = User => Context => Request[_] => Future[Result]

  /**
   * Type for the callback for user aware actions (synchronous)
   */
  type userAwareControllerCallback = Option[User] => Context => Request[_] => Result

  /**
   * Builds an action using a simple function that takes user, context and request as parameters
   * @param tags
   * @param block
   * @return
   */
  def securedContextAction(tags: String*)(block: securedControllerCallback): Action[AnyContent] =
    ContextAction(tags: _*) {
      SecuredAction.async { r: SecuredRequest[_] => r match {
        case SecuredRequest(user, authenticator, ContextRequest(ctx, originalRequest)) =>
          block(user)(ctx)(originalRequest)
        }
      }
    }

  /**
   * Builds an action using a simple function that takes a user option, context and request as parameters
   * @param tags
   * @param block
   * @return
   */
  def userAwareContextAction(tags: String*)(block: userAwareControllerCallback): Action[AnyContent] =
    ContextAction(tags: _*) {
      UserAwareAction { r: RequestWithUser[_] => r match {
        case RequestWithUser(user, authenticator, ContextRequest(ctx, originalRequest)) =>
          block(user)(ctx)(originalRequest)
      }
      }
    }

}
