package ems.backend.cms

import io.prismic.{DocumentLinkResolver, Document}

import scala.concurrent.Future

/**
 * A service that fetches content from prismic CMS
 */
trait PrismicService {

  /**
   * Returns all documents matching tags
   * @return
   */
  def getDocuments(tags: String*): Future[Map[String, Seq[Document]]]

  /**
   * Builds a link resolver
   * @return
   */
  def getLinkResolver: Future[DocumentLinkResolver]

}
