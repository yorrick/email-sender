package ems.controllers.utils

import io.prismic.{DocumentLinkResolver, Document}


case class Context(prismicContext: Option[PrismicContext]) {

  /**
   * Returns the first prismic document with this tag, if possible
   * @param tag
   */
  def firstDocument(tag: String): Option[(Document, DocumentLinkResolver)] =
    prismicContext flatMap { pc => pc.firstOf(tag) map { (_, pc.linkResolver)}}
}
