package ems.controllers.utils

import controllers.{WebJarAssets, Assets}
import controllers.Assets.Asset
import play.api.mvc.{AnyContent, Call, Action, Controller}
import play.api.Play.current


/**
 * This controller allows to serve webjars either locally, or through a CDN
 * See http://www.jamesward.com/2014/03/20/webjars-now-on-the-jsdelivr-cdn
 *
 * Since this controller must be easily accessed from templates, it is not an injectable scaldi class controller.
 */
object CDNAssets extends Controller {

  val webJarCDNUrl = current.configuration.getString("ems.controllers.utils.CDNAssets.webJarCDNUrl")
  val resourceCDNUrl = current.configuration.getString("ems.controllers.utils.CDNAssets.resourceCDNUrl")

  /**
   * Used in routes to generate the action
   * @param file
   * @return
   */
  def webJarAt(file: String): Action[AnyContent] = Assets.at("/META-INF/resources/webjars", file)

  /**
   * For webjars, given parameter does not contain the full path yet, we have to use "webJarAt"
   * @param file
   * @return
   */
  def webJarUrl(file: String) = webJarCDNUrl map { url =>
      url + ems.controllers.utils.routes.CDNAssets.webJarAt(file).url
    } getOrElse ems.controllers.utils.routes.CDNAssets.webJarAt(file).url


  /**
   * For internal resources we just have to prepend CDN name
   * @param call
   * @return
   */
  def resourceUrl(call: Call) = {
    val result = resourceCDNUrl map { _ + call.url } getOrElse call.url
    // work around for https://github.com/playframework/playframework/pull/3074
    result.replaceAllLiterally("//", "/")

    // adds double slash automatically 
    "//" + result
  }

}
