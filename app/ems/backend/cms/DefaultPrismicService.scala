package ems.backend.cms

import io.prismic._
import play.api.Logger
import scaldi.{Injectable, Injector}

import scala.concurrent.{Future, ExecutionContext}


class DefaultPrismicService(implicit inj: Injector) extends PrismicService with Injectable {

  implicit val executionContext = inject[ExecutionContext]
  val prismicApiUrl = inject[String] (identified by "ems.controllers.MainController.prismic.api")

  /**
   * Write debug and error messages to the Play `prismic` logger (check the configuration in application.conf)
   */
  private val logger = (level: Symbol, message: String) => level match {
    case 'DEBUG => Logger("prismic").debug(message)
    case 'ERROR => Logger("prismic").error(message)
    case _      => Logger("prismic").info(message)
  }

  /**
   * Builds the cache object
   */
  private val cache = BuiltInCache(200)

  /**
   * Builds api object
   * @return
   */
  val apiFuture: Future[Api] = Api.get(prismicApiUrl, cache = cache, logger = logger)

  /**
   * TODO implement this
   * Resolve links to documents
   */
  val getLinkResolver: Future[DocumentLinkResolver] = apiFuture map { api=> DocumentLinkResolver(api) {
    case (Fragment.DocumentLink(id, docType, tags, slug, false), maybeBookmarked) => ""
    case (link @ Fragment.DocumentLink(_, _, _, _, true), _)                      => ""
  }}

  def getDocuments(tags: String*) = {
    val wrappedTags = tags map {"\"" + _ + "\""} mkString(", ")
    val query = s"""[[:d = any(document.tags, [$wrappedTags])]]"""

    val docListFuture = for {
      api <- apiFuture
      documentList <- api.forms("everything").query(query).ref(api.master).submit() map (_.results)
    } yield documentList

    docListFuture map { docList =>
      // warning: we iterate N times over the list, where N is the number of tags asked for
      // this is not a problem in this case since we have very small numbers of tags, but this could be improved
      val r: Seq[(String, List[Document])] = tags map { searchedTag: String =>
        searchedTag -> (docList filter { _.tags.contains(searchedTag)})
      }

      r.toMap

    }

  }

}
