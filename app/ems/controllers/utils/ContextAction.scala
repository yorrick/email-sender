package ems.controllers.utils

import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future


//case class ContextAction[A](action: Action[A]) extends Action[A] {
//
//  def apply(request: Request[A]): Future[Result] = {
//    Logger.info("=========================Calling ContextAction")
//    action(request)
//  }
//
//  lazy val parser = action.parser
//}



//object ContextActionBuilder extends ActionBuilder[Request] {
//  def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
//    block(request)
//  }
//  override def composeAction[A](action: Action[A]) = new ContextAction(action)
//}

case class Context(theValue: String)

object FakeContext extends Context("toto")

case class ContextRequest[A](val context: Context, request: Request[A]) extends WrappedRequest[A](request)

//object ContextAction extends ActionBuilder[ContextRequest] with ActionTransformer[Request, ContextRequest] {
//  def transform[A](request: Request[A]) = Future.successful {
//    new ContextRequest(FakeContext, request)
//  }
//}

//object ContextAction extends ActionBuilder[ContextRequest] {
//
//  def invokeBlock[A](request: Request[A], block: (ContextRequest[A]) => Future[Result]): Future[Result] =
//    request match {
//      case r: TokenRequest[A] => checkPermissions(r, permissions.toSeq, block)
//      case _ => resolve(InternalServerError)
//    }
//
//  override def composeAction[A](action: Action[A]) = hasToken.async(action.parser) { tokenRequest =>
//    action(tokenRequest)
//  }
//
//}

case class ContextAction[A](action: Action[A]) extends Action[A] {

  def apply(request: Request[A]): Future[Result] = {
    action(new ContextRequest(FakeContext, request))
  }

  lazy val parser = action.parser
}

