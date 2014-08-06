package ems.views.auth


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.backend.utils.EMSRuntimeEnvironment


@RunWith(classOf[JUnitRunner])
class LoginSpec extends PlaySpecification {
  sequential

  "Login view" should {

    "Be generated" in new WithApplication() {
      implicit val requestHeader = FakeRequest()
      implicit val env = EMSRuntimeEnvironment.instance

      val result = ems.views.html.auth.login(None)
      contentAsString(result) must contain("Google")
    }

  }
}
