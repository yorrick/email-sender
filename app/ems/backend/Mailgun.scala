package ems.backend

import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WSResponse, WSAuthScheme, WS, WSRequestHolder}
import play.api.http.Status


/**
 * Utility that send emails using mailgun http api.
 */
object Mailgun {

  val key = current.configuration.getString("mailgun.api.key")
  // we retrieve the domain from the smtp login since heroku mailgun does not give the domain alone
  val domain = current.configuration.getString("mailgun.smtp.login").map(_.split("@").last)
  val apiUrl = current.configuration.getString("mailgun.api.url") match {
    case some @ Some(url) => some
    case None =>
      domain map {domain => s"https://api.mailgun.net/v2/${domain}/messages"}
  }

  lazy val missingCredentials = new Exception(s"Missing credentials: key ($key) or domain ($domain)")

  val DELIVERED = "delivered"

  def requestHolderOption: Option[WSRequestHolder] = for {
    key <- key
    apiUrl <- apiUrl
  } yield WS.url(apiUrl).withAuth("api", key, WSAuthScheme.BASIC)

  /**
   * Mailgun call will never reply with a failure, but with an forwarding that contains the updated status
   * @param from
   * @param to
   * @param content
   * @return mailgunId
   */
  def sendEmail(from: String, to: String, content: String): Future[String] = {

    val postData = Map(
      "from" -> Seq(s"${from}@${domain.get}"),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(content)
    )

    val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
      requestHolder.post(postData)
    } getOrElse Future.failed(missingCredentials)

    val okResponse = responseFuture filter { _.status == Status.OK }
    okResponse flatMap { response => handleMailgunResponse(response.json)}
  }

  /**
   * Returns the given mailgunId
   *
   * Mailgun response looks like this
   *       {
   *         "message": "Queued. Thank you.",
   *         "id": "<20140719141813.41030.12232@emsdev.mailgun.org>"
   *       }
   *
   * @param json
   * @return
   */
  private def handleMailgunResponse(json: JsValue): Future[String] = {
    (json \ "id").validate[String] match {
      case JsSuccess(id, _) =>
        Logger.debug(s"Mailgun response id: $id")
        Future.successful(id)
      case error @ JsError(_) =>
        Future.failed(new Exception(error.toString))
    }

  }
}
