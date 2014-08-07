package ems.backend

import ems.models.Forwarding
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{Handler, Action}
import play.api.test._
import play.api.mvc.Results.Ok

import ems.utils.WithMongoTestData


@RunWith(classOf[JUnitRunner])
class MailgunSpec extends PlaySpecification with WithMongoTestData {
  sequential

  /**
   * Intercepts all POST made to the application
   */
  val routes: PartialFunction[(String, String), Handler] = {
    case ("POST", _: String) =>
      Action { Ok("ok") }
  }

  val app = FakeApplication(withRoutes = routes)

  "Mailgun" should {

    "Send emails" in new WithServer(app = app, port = 3333) {
      await(Mailgun.sendEmail(forwarding, "nobody@nobody.com")).id must beEqualTo(forwarding.id)
    }

  }
}
