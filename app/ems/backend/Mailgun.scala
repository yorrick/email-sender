package ems.backend

import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS, WSRequestHolder}
import play.api.http.Status

import ems.models._


/**
 * Utility that send emails using mailgun http api.
 */
object Mailgun {

  val key = current.configuration.getString("mailgun.api.key")
  // we retrieve the domain from the smtp login since heroku mailgun does not give the domain alone
  val domain = current.configuration.getString("mailgun.smtp.login").map(_.split("@").last)
  val apiUrl = domain map {domain => s"https://api.mailgun.net/v2/${domain}/messages"}
  lazy val missingCredentials = new Exception(s"Missing credentials: key ($key) or domain ($domain)")

  val DELIVERED = "delivered"

  def requestHolderOption: Option[WSRequestHolder] = for {
    key <- key
    apiUrl <- apiUrl
  } yield WS.url(apiUrl).withAuth("api", key, WSAuthScheme.BASIC)

  /**
   * Mailgun call will never reply with a failure, but with an sms that contains the updated status
   * @param sms
   * @param to
   * @return
   */
  def sendEmail(sms: Sms, to: String = "yorrick.jansen@gmail.com"): Future[Sms] = {

    val postData = Map(
      "from" -> Seq(to),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(sms.content)
    )

    val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
      requestHolder.post(postData)
    } getOrElse Future.failed(missingCredentials)

    val okResponse = responseFuture filter { _.status == Status.OK }
    val smsResponse = okResponse flatMap { response => handleMailgunResponse(sms, response.json)}

    // in case something went wrong
    smsResponse recover {
      case _ => sms.withStatus(NotSentToMailgun)
    }
  }

  /**
   * Returns the given sms with updated status if everything went fine
   *
   * Mailgun response looks like this
   *       {
   *         "message": "Queued. Thank you.",
   *         "id": "<20140719141813.41030.12232@emsdev.mailgun.org>"
   *       }
   *
   * @param sms
   * @param json
   * @return
   */
  private def handleMailgunResponse(sms: Sms, json: JsValue): Future[Sms] = {
    (json \ "id").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Mailgun response id: $id")
        SmsStore.updateSmsMailgunId(sms.withMailgunId(id))
        Future.successful(sms.withStatus(SentToMailgun))
      case error @ JsError(_) =>
        Future.failed(new Exception(error.toString))
    }

  }
}
