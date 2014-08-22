package ems.backend


import ems.backend.sms.TwilioService
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{Handler, Action}
import play.api.test._
import play.api.mvc.Results.Created

import ems.utils.{AppInjector, WithMongoTestData}
import scaldi.Injectable


@RunWith(classOf[JUnitRunner])
class TwilioServiceSpec extends PlaySpecification with WithMongoTestData with AppInjector with Injectable {
  sequential

  val resultTwilioId = "SMa3491ba7cff849649150a1aea8aa3575"

  val fakeTwilioResponse = s"""{
     |       "sid":"${resultTwilioId}",
     |       "date_created":"Mon, 11 Aug 2014 21:25:04 +0000",
     |       "date_updated":"Mon, 11 Aug 2014 21:25:04 +0000",
     |       "date_sent":null,
     |       "account_sid":"AC791403f3ec0098401e629d6aaf6b44bd",
     |       "to":"+14387639474",
     |       "from":"+14387938597",
     |       "body":"Hello from email",
     |       "status":"queued",
     |       "num_segments":"1",
     |       "num_media":"0",
     |       "direction":"outbound-api",
     |       "api_version":"2010-04-01",
     |       "price":null,
     |       "price_unit":"USD",
     |       "error_code":null,
     |       "error_message":null,
     |       "uri":"/2010-04-01/Accounts/AC791403f3ec0098401e629d6aaf6b44bd/Messages/SMa3491ba7cff849649150a1aea8aa3575.json",
     |       "subresource_uris":{
     |          "media":"/2010-04-01/Accounts/AC791403f3ec0098401e629d6aaf6b44bd/Messages/SMa3491ba7cff849649150a1aea8aa3575/Media.json"
     |       }
     |    }""".stripMargin

  /**
   * Intercepts all POST made to the application
   */
  val routes: PartialFunction[(String, String), Handler] = {
    case ("POST", _: String) =>
      Action { Created(fakeTwilioResponse) }
  }

  val app = FakeApplication(withRoutes = routes)

  "Twilio" should {

    "Send confirmation sms" in new WithServer(app = app) {
      implicit val injector = appInjector
      val service = inject[TwilioService]

      await(service.sendConfirmationSms("+15140000000")) must beEqualTo(resultTwilioId)
    }

  }
}
