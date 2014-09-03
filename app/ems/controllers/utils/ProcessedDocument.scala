package ems.controllers.utils

import io.prismic.Document


case class ProcessedDocument(document: Document, html: String)
