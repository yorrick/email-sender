package ems.controllers.utils


import io.prismic.{DocumentLinkResolver, Document}


case class PrismicContext(documents: Map[String, Seq[Document]], linkResolver: DocumentLinkResolver) {

  /**
   * Returns the first document with this tag
   * @param tag
   */
  def firstOf(tag: String): Option[Document] = documents.get(tag) flatMap  { _.headOption}

}
