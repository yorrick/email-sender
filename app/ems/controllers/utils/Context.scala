package ems.controllers.utils

import io.prismic.{DocumentLinkResolver, Document}


case class Context(documents: Map[String, Seq[Document]], linkResolver: DocumentLinkResolver)
