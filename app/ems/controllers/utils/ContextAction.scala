package ems.controllers.utils

import ems.backend.cms.PrismicService
import io.prismic.{DocumentLinkResolver, Document}
import play.api.Logger
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

  /**
   * Seq of tags that are loaded by default in context
   * We inject tags this way because scaldi play integration does not support Seq or List injection
   */
  val defaultTags: Seq[String] = (inject[String] (identified by "ems.controllers.utils.ContextAction.defaultTags")).split(",")

  /**
   * Action is executed after prismic answers.
   * If prismic fails on way or another, we do NOT want to be dpwn as well!
   * This is why we recover any error that shows up at this point.
   *
   * @param request
   * @return
   */
  def apply(request: Request[A]): Future[Result] = {
    // remove any duplicate
    val allTags: Set[String] = defaultTags.toSet ++ prismicTags

    val prismicResponse = for {
      docs <- prismicService.getDocuments(allTags.toSeq: _*)
      linkResolver <- prismicService.getLinkResolver
    } yield Some(PrismicContext(docs, linkResolver))

    prismicResponse recover {
      case t: Throwable =>
        Logger.warn(s"Error calling prismic API: $t")
        None
    } flatMap { case pc: Option[PrismicContext] =>
      action(new ContextRequest(Context(pc), request))
    }

  }

  lazy val parser = action.parser
}
