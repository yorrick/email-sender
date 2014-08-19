package ems.backend

import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{Handler, Action}
import play.api.test._
import play.api.mvc.Results.Ok

import ems.utils.WithMongoTestData
import scaldi.Injectable
import scaldi.play.ScaldiSupport


@RunWith(classOf[JUnitRunner])
class MailgunSpec extends PlaySpecification with WithMongoTestData with Injectable {
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

  val app = FakeApplication(withRoutes = routes)

  "Mailgun" should {

    "Send emails" in new WithServer(app = app) {
      implicit val injector = app.global.asInstanceOf[ScaldiSupport].injector
      val mailgun = inject[MailgunService]

      await(mailgun.sendEmail("5140000000", "nobody@nobody.com", "Some content")) must beEqualTo(resultMailgunId)
    }

  }
}
