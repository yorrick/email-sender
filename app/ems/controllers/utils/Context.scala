package ems.controllers.utils

import io.prismic.Document


case class ProcessedDocument(document: Document, html: String)


case class Context(document: Option[ProcessedDocument])
