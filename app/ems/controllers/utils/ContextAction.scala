package ems.controllers.utils

import ems.backend.cms.PrismicService
import io.prismic.{DocumentLinkResolver, Document}
import play.api.mvc._
import scaldi.{Injectable, Injector}
import scala.concurrent.Future


//class ContextUtils(implicit inj: Injector) extends Injectable {
//
//  val prismicService = inject[PrismicService]
//
//  def buildAction[A](prismicTag: String) = {
//    for {
//      document <- prismicService.getMainPageDocument
//    } yield {
//      new Action[A] {
//
//        def apply(request: Request[A]): Future[Result] =
//          for {
//            linkResolver <- prismicService.getLinkResolver
//            result <- action(new ContextRequest(Context(prismicService), request))
//          } yield {
//            result
//          }
//
//        lazy val parser = action.parser
//      }
//    }
//  }
//}

//case class ContextAction[A](action: Action[A]) extends Action[A] {
//
//  def apply(request: Request[A]): Future[Result] =
//    for {
//      linkResolver <- prismicService.getLinkResolver
//      result <- action(new ContextRequest(Context(prismicService), request))
//    } yield {
//      result
//    }
//
//  lazy val parser = action.parser
//}


case class ContextAction[A](prismicTag: String)(action: Action[A])(implicit inj: Injector) extends Action[A] with Injectable {

  val prismicService = inject[PrismicService]
  implicit def ec = executionContext

  def buildProcessedDocument(document: Option[Document], linkResolver: DocumentLinkResolver): Option[ProcessedDocument] =
    document map { doc => ProcessedDocument(doc, doc.asHtml(linkResolver)) }

  def apply(request: Request[A]): Future[Result] =
    for {
      doc <- prismicService.getMainPageDocument
      linkResolver <- prismicService.getLinkResolver
      result <- action(new ContextRequest(Context(buildProcessedDocument(doc, linkResolver)), request))
    } yield {
      result
    }

  lazy val parser = action.parser
}

//case class ContextAction[A](action: Action[A]) extends Action[A] {
//
//  def apply(request: Request[A]): Future[Result] = {
//    val ctx = null
//    action(new ContextRequest(ctx, request))
//  }
//
//  lazy val parser = action.parser
//}

