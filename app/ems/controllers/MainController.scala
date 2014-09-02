package ems.controllers

import ems.backend.cms.PrismicService
import scaldi.{Injectable, Injector}
import securesocial.core._
import securesocial.core.SecureSocial
import ems.models.User
import scala.concurrent.ExecutionContext


class MainController(implicit inj: Injector) extends SecureSocial[User] with Injectable {

  override implicit val env = inject [RuntimeEnvironment[User]]
  implicit val executionContext = inject[ExecutionContext]
  val cmsService = inject[PrismicService]

  def index = UserAwareAction.async { implicit request =>
    implicit val user = request.user

    for {
      doc <- cmsService.getMainPageDocument
      linkResolver <- cmsService.getLinkResolver
    } yield Ok(ems.views.html.index(doc, linkResolver))
  }

}
