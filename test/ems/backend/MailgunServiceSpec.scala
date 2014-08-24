package ems.backend

import ems.backend.email.MailgunService
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{Handler, Action}
import play.api.test._
import play.api.mvc.Results.Ok

import ems.utils.{MockUtils, AppInjector, WithTestData}
import scaldi.Injectable
import scaldi.play.ScaldiSupport


@RunWith(classOf[JUnitRunner])
class MailgunServiceSpec extends PlaySpecification with WithTestData with Injectable with AppInjector with MockUtils {
  sequential

  val resultMailgunId = "<xxxxxxxx@xxxx.mailgun.org>"

  val fakeMailgunResponse =
    s"""{"message": "Queued. Thank you.","id": "${resultMailgunId}"}"""

  /**
   * Intercepts all POST made to the application
   */
  val routes: PartialFunction[(String, String), Handler] = {
    case ("POST", _: String) =>
      Action { Ok(fakeMailgunResponse) }
  }

  override val app = FakeApplication(withRoutes = routes, withoutPlugins = Seq(mongoPluginClass, redisPluginClass))

  "Mailgun" should {

    "Send emails" in new WithServer(app = app) {
      implicit val injector = appInjector
      val mailgun = inject[MailgunService]

      await(mailgun.sendEmail("5140000000", "nobody@nobody.com", "Some content")) must beEqualTo(resultMailgunId)
    }

  }
}
