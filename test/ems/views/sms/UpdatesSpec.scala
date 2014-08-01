package ems.views.sms

import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import play.twirl.api.JavaScriptFormat


@RunWith(classOf[JUnitRunner])
class UpdatesSpec extends PlaySpecification {
  sequential

  "Javascript updates" should {

    "Be generated" in new WithApplication() {
      implicit val requestHeader = FakeRequest()
      val javascript: JavaScriptFormat.Appendable = ems.views.js.sms.updates()
      javascript.toString must contain("chatSocket.onmessage")
    }

  }
}
