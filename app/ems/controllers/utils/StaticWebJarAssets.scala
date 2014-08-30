package ems.controllers.utils

import controllers.Assets
import play.api.mvc.Controller
import play.api.Play.current


/**
 * This controller allows to serve webjars either locally, or through a CDN
 * See http://www.jamesward.com/2014/03/20/webjars-now-on-the-jsdelivr-cdn
 *
 * Since this controller must be easily accessed from templates, it is not an injectable scaldi class controller.
 */
object StaticWebJarAssets extends Controller {

  val maybeContentUrl = current.configuration.getString("ems.controllers.utils.StaticWebJarAssets.contentUrl")

  def at(file: String) = Assets.at("/META-INF/resources/webjars", file)

  def getUrl(file: String) = {
    maybeContentUrl map { contentUrl =>
      contentUrl + ems.controllers.utils.routes.StaticWebJarAssets.at(file).url
    } getOrElse ems.controllers.utils.routes.StaticWebJarAssets.at(file).url
  }

}
