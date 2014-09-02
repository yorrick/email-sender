package ems.backend.cms

import io.prismic.{DocumentLinkResolver, Document}

import scala.concurrent.Future

/**
 * A service that fetches content from prismic CMS
 */
trait PrismicService {

  /**
   * Returns the document that will be displayed on the main page
   * @return
   */
  def getMainPageDocument: Future[Option[Document]]

  /**
   * Builds a link resolver
   * @return
   */
  def linkResolver: Future[DocumentLinkResolver]

}
