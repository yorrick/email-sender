package ems.backend.sms

import ems.backend.utils.LogUtils
import play.api.Logger
import play.api.Play.current
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.libs.ws.{WS, WSAuthScheme, WSRequestHolder, WSResponse}
import scaldi.{Injector, Injectable}

import scala.concurrent.{ExecutionContext, Future}


/**
 * Contains utilities to connect to Twilio
 */
class DefaultTwilioService(implicit inj: Injector) extends LogUtils with TwilioService with Injectable {

  val apiMainNumber = inject[String] (identified by "ems.backend.sms.DefaultTwilioService.apiMainNumber")
  val apiUrl = inject[String] (identified by "ems.backend.sms.DefaultTwilioService.apiUrl")
  val apiSid = inject[String] (identified by "ems.backend.sms.DefaultTwilioService.apiSid")
  val apiToken = inject[String] (identified by "ems.backend.sms.DefaultTwilioService.apiToken")
  implicit val executionContext = inject[ExecutionContext]

  /**
   * Builds a request holder object
   * @return
   */
  def requestHolder = WS.url(apiUrl).withAuth(apiSid, apiToken, WSAuthScheme.BASIC)

  val confirmationMessage = s"Welcome to email-sender! You can start using this service by sending sms to ${apiMainNumber}"

  /**
   * Send sms using twilio api
   * Returns true if the confirmation message was sent successfuly
   * @param to
   * @return
   */
  def sendConfirmationSms(to: String): Future[String] = {
    sendSms(to, confirmationMessage)
  }

  /**
   * Send sms using twilio api
   * @param to
   * @return
   */
  def sendSms(to: String, content: String): Future[String] = {

    val postData = Map(
      "To" -> Seq(to),
      "From" -> Seq(apiMainNumber),
      "Body" -> Seq(content)
    )

    val responseFuture: Future[WSResponse] = requestHolder.post(postData)
    val okResponse = responseFuture andThen logResult("twilioResponse", extractor = {r => s"${r.status}: ${r.body}"}) filter { _.status == Status.CREATED }
    okResponse flatMap { response => handleTwilioResponse(response.json)}
  }

  /**
   * Returns the given mailgunId
   *
      {
       "sid":"SMa3491ba7cff849649150a1aea8aa3575",
       "date_created":"Mon, 11 Aug 2014 21:25:04 +0000",
       "date_updated":"Mon, 11 Aug 2014 21:25:04 +0000",
       "date_sent":null,
       "account_sid":"AC791403f3ec0098401e629d6aaf6b44bd",
       "to":"+14387639474",
       "from":"+14387938597",
       "body":"Hello from email",
       "status":"queued",
       "num_segments":"1",
       "num_media":"0",
       "direction":"outbound-api",
       "api_version":"2010-04-01",
       "price":null,
       "price_unit":"USD",
       "error_code":null,
       "error_message":null,
       "uri":"/2010-04-01/Accounts/AC791403f3ec0098401e629d6aaf6b44bd/Messages/SMa3491ba7cff849649150a1aea8aa3575.json",
       "subresource_uris":{
          "media":"/2010-04-01/Accounts/AC791403f3ec0098401e629d6aaf6b44bd/Messages/SMa3491ba7cff849649150a1aea8aa3575/Media.json"
       }
    }
   *
   * @param json
   * @return
   */
  private def handleTwilioResponse(json: JsValue): Future[String] = {
    (json \ "sid").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Twilio response id: $id")
        Future.successful(id)
      case error @ JsError(_) =>
        Logger.debug(s"Could not read twilio response id")
        Future.failed(new Exception(error.toString))
    }

  }
}
