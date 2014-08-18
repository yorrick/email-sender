package ems.utils.securesocial


import ems.modules.WebModule
import ems.utils.TestModule
import play.api.mvc.Cookie

import ems.backend.{WithGlobal, Global}
import play.api.test.FakeApplication
import scaldi.play.ControllerInjector


trait WithSecureSocialUtils {

  /**
   * Cookie used for tests.
   * The value is not used as the AuthenticationStore is mocked
   * TODO find a way to use configuration (we need an app in context to be able to use CookieAuthenticator.cookieName)
   */
  lazy val cookie = Cookie("emailsenderid", "")

  object TestGlobal extends WithGlobal {
    override def applicationModule = new TestModule :: new WebModule :: new ControllerInjector
  }

  /**
   * An application with an always logged in user
   */
  def app = FakeApplication(withGlobal = Some(TestGlobal))
}