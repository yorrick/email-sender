package ems.backend


import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.http.Status
import play.api.libs.ws.{WSAuthScheme, WS, WSRequestHolder, WSResponse}
import play.api.libs.concurrent.Execution.Implicits._

import ems.models.{NotSentToMailgun, Sms}


/**
 * Contains utilities to connect to Twilio
 */
object Twilio {

//  curl -XPOST https://api.twilio.com/2010-04-01/Accounts/<SID>/Messages.json
// --data-urlencode "Body=Hello there"
// --data-urlencode "To=<+15140000000>"
// --data-urlencode "From=+14387938597"
// -u '<SID>:<TOKEN>'

  val apiMainNumber = current.configuration.getString("twilio.api.mainNumber").get
  val apiUrl = current.configuration.getString("twilio.api.url")
  val apiSid = current.configuration.getString("twilio.api.sid")
  val apiToken = current.configuration.getString("twilio.api.token")
  lazy val missingCredentials = new Exception(s"Missing credentials: url ($apiUrl) or key ($apiSid) or domain ($apiToken)")

  /**
   * Builds a request holder object
   * @return
   */
  def requestHolderOption: Option[WSRequestHolder] = for {
    apiUrl <- apiUrl
    apiSid <- apiSid
    apiToken <- apiToken
  } yield WS.url(apiUrl).withAuth(apiSid, apiToken, WSAuthScheme.BASIC)

  val confirmationMessage = s"Welcome to email-sender! You can start using this service by sending sms to ${apiMainNumber}"

  /**
   * Send sms using twilio api
   * Returns true if the confirmation message was sent successfuly
   * @param to
   * @return
   */
  def sendConfirmationSms(to: String): Future[Boolean] = {

    val postData = Map(
      "To" -> Seq(to),
      "From" -> Seq(apiMainNumber),
      "Body" -> Seq(confirmationMessage)
    )

    val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
      requestHolder.post(postData)
    } getOrElse Future.failed(missingCredentials)

    responseFuture map { _.status == Status.OK }
  }

}
