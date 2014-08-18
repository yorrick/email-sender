package ems.views.auth


import ems.backend.Global
import ems.models.User
import ems.modules.WebModule
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import scaldi.Injectable
import securesocial.core.RuntimeEnvironment


@RunWith(classOf[JUnitRunner])
class LoginSpec extends PlaySpecification with Injectable {
  sequential

  implicit val injector = new WebModule

  "Login view" should {

    "Be generated" in new WithApplication() {
      implicit val requestHeader = FakeRequest()
      implicit val env = inject[RuntimeEnvironment[User]]

      val result = ems.views.html.auth.login(None)
      contentAsString(result) must contain("Google")
    }

  }
}
