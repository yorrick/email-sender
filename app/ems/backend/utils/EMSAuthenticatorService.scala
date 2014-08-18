package ems.backend.utils

import ems.models.User
import play.api.mvc.RequestHeader
import scaldi.{Injectable, Injector}
import securesocial.core.authenticator._
import securesocial.core.services.AuthenticatorService

import scala.concurrent.{ExecutionContext, Future}


/**
 * Authenticator service for the project
 * It only uses Cookies
 * We had to rewrite all code to ensure methods are using injected builders and not the parent's
 */
class EMSAuthenticatorService(implicit inj: Injector) extends AuthenticatorService[User] with Injectable {

//  val builders = Seq(new CookieAuthenticatorBuilder[User](store, new IdGenerator.Default()))
  val builders = Seq(inject[EMSCookieAuthenticatorBuilder])

  override val asMap = builders.map { builder => builder.id -> builder }.toMap

  override def find(id: String): Option[AuthenticatorBuilder[User]] = {
    println(s"===========================Trying to find auth builder $id")
    super.find(id)
  }

  override def fromRequest(implicit request: RequestHeader): Future[Option[Authenticator[User]]] = {
    import ExecutionContext.Implicits.global
    println(s"========================= Building authenticator fromRequest: ${request.cookies}, ${asMap}")

    def iterateIt(seq: Seq[AuthenticatorBuilder[User]]): Future[Option[Authenticator[User]]] = {
      if ( seq.isEmpty )
        Future.successful(None)
      else {
        seq.head.fromRequest(request).flatMap {
          case Some(authenticator) => Future.successful(Some(authenticator))
          case None => iterateIt(seq.tail)
        }
      }
    }
    iterateIt(builders)
  }
}