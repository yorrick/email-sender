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
   * Mailgun call will never reply with a failure, but with an forwarding that contains the updated status
   * @param forwarding
   * @param to
   * @return
   */
  def sendEmail(forwarding: Forwarding, to: String): Future[Forwarding] = {

    val postData = Map(
      "from" -> Seq(to),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(forwarding.content)
    )

    val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
      requestHolder.post(postData)
    } getOrElse Future.failed(missingCredentials)

    val okResponse = responseFuture filter { _.status == Status.OK }
    val forwardingResponse = okResponse flatMap { response => handleMailgunResponse(forwarding, response.json)}

    // in case something went wrong
    forwardingResponse recover {
      case t: Throwable =>
        Logger.warn(s"Could not send forwarding to mailgun: $t")
        forwarding.withStatus(NotSentToMailgun)
    }
  }

  /**
   * Returns the given forwarding with updated status if everything went fine
   *
   * Mailgun response looks like this
   *       {
   *         "message": "Queued. Thank you.",
   *         "id": "<20140719141813.41030.12232@emsdev.mailgun.org>"
   *       }
   *
   * @param forwarding
   * @param json
   * @return
   */
  private def handleMailgunResponse(forwarding: Forwarding, json: JsValue): Future[Forwarding] = {
    (json \ "id").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Mailgun response id: $id")
        ForwardingStore.updateForwardingMailgunId(forwarding.withMailgunId(id))
        Future.successful(forwarding.withStatus(SentToMailgun))
      case error @ JsError(_) =>
        Future.failed(new Exception(error.toString))
    }

  }
}
