package ems.controllers

import play.api.mvc.RequestHeader
import play.twirl.api.Html
import play.api.data.Form
import play.api.i18n.Lang

import securesocial.core.RuntimeEnvironment
import securesocial.controllers.{ChangeInfo, RegistrationInfo, ViewTemplates}


class EMSViewTemplates(env: RuntimeEnvironment[_]) extends ViewTemplates {
  implicit val implicitEnv = env

  override def getLoginPage(form: Form[(String, String)],
                            msg: Option[String] = None)(implicit request: RequestHeader, lang: Lang): Html = {
//    securesocial.views.html.login(form, msg)(request, lang, env)
    ems.views.html.auth.login(form, msg)(request, lang, env)
  }

  override def getSignUpPage(form: Form[RegistrationInfo], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.Registration.signUp(form, token)(request, lang, env)
  }

  override def getStartSignUpPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.Registration.startSignUp(form)(request, lang, env)
  }

  override def getStartResetPasswordPage(form: Form[String])(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.Registration.startResetPassword(form)(request, lang, env)
  }

  override def getResetPasswordPage(form: Form[(String, String)], token: String)(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.Registration.resetPasswordPage(form, token)(request, lang, env)
  }

  override def getPasswordChangePage(form: Form[ChangeInfo])(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.passwordChange(form)(request, lang, env)
  }

  def getNotAuthorizedPage(implicit request: RequestHeader, lang: Lang): Html = {
    securesocial.views.html.notAuthorized()(request, lang, env)
  }
}