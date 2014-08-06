package ems.backend


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{Handler, Action}
import play.api.test._
import play.api.mvc.Results.Ok

import ems.models.{AckedByMailgun, SentToMailgun}
import ems.utils.{WithMongoData, WithMongoTestData}


@RunWith(classOf[JUnitRunner])
class TwilioSpec extends PlaySpecification with WithMongoTestData {
  sequential

  /**
   * Intercepts all POST made to the application
   */
  val routes: PartialFunction[(String, String), Handler] = {
    case ("POST", _: String) =>
      Action { Ok("ok") }
  }

  val app = FakeApplication(withRoutes = routes)

  "Twilio" should {

    "Send confirmation sms" in new WithServer(app = app, port = 3333) {
      await(Twilio.sendConfirmationSms("+15140000000")) must beTrue
    }

  }
}
