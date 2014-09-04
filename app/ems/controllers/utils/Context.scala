package ems.controllers.utils

import io.prismic.{DocumentLinkResolver, Document}


case class Context(prismicContext: Option[PrismicContext])


case class PrismicContext(documents: Map[String, Seq[Document]], linkResolver: DocumentLinkResolver)