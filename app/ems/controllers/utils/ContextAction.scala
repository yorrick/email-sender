package ems.controllers.utils

import ems.backend.cms.PrismicService
import io.prismic.{DocumentLinkResolver, Document}
import play.api.mvc._
import scaldi.{Injectable, Injector}
import scala.concurrent.Future


/**
 * Adds a Context to request object
 * @param prismicTags
 * @param action
 * @param inj
 * @tparam A
 */
case class ContextAction[A](prismicTags: String*)(action: Action[A])(implicit inj: Injector) extends Action[A] with Injectable {

  val prismicService = inject[PrismicService]
  implicit def ec = executionContext

  def buildProcessedDocument(documents: Map[String, Seq[Document]], linkResolver: DocumentLinkResolver) =
    documents map { case (tag, docs) =>
      val pDocs = docs map { doc => ProcessedDocument(doc, doc.asHtml(linkResolver))}
      (tag, pDocs)
    }

  /**
   * action is executed after prismic answers
   * @param request
   * @return
   */
  def apply(request: Request[A]): Future[Result] =
    for {
      docs <- prismicService.getDocuments(prismicTags: _*)
      linkResolver <- prismicService.getLinkResolver
      result <- action(new ContextRequest(Context(buildProcessedDocument(docs, linkResolver)), request))
    } yield {
      result
    }

  lazy val parser = action.parser
}
