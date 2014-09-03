package ems.controllers.auth

import ems.controllers.utils.{ControllerUtils, Context}
import ems.models.User
import play.api.Logger
import play.api.data.Form
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import scaldi.{Injectable, Injector}
import securesocial.controllers.{ProviderControllerHelper, BaseLoginPage}
import securesocial.core.providers.UsernamePasswordProvider
import securesocial.core.{SecureSocial, RuntimeEnvironment}


/**
 * Overrides the login page to integrate this controller into scaldi DI
 * @param injector
 */
class LoginController(implicit val injector: Injector) extends BaseLoginPage[User] with ControllerUtils with Injectable {
  override implicit val env = inject [RuntimeEnvironment[User]]

//  override def login = UserAwareAction { implicit request =>
  override def login = userAwareContextAction("footer") { implicit user => implicit ctx => implicit request =>
    val to = ProviderControllerHelper.landingUrl
    if ( user.isDefined ) {
      // if the user is already logged in just redirect to the app
      Logger.debug("User already logged in, skipping login page. Redirecting to %s".format(to))
      Redirect( to )
    } else {
      if ( SecureSocial.enableRefererAsOriginalUrl ) {
        SecureSocial.withRefererAsOriginalUrl(Ok(getLoginPage(UsernamePasswordProvider.loginForm)))
      } else {
        Ok(getLoginPage(UsernamePasswordProvider.loginForm))
      }
    }
  }

  def getLoginPage(form: Form[(String, String)], msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang, ctx: Context): Html = {
    ems.views.html.auth.login(msg)(request, lang, env, ctx)
  }


}
