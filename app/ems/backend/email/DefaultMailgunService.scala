package ems.backend.email

import play.api.Logger
import play.api.http.Status
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import play.api.libs.ws.{WS, WSAuthScheme, WSRequestHolder, WSResponse}
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

/**
 * Utility that send emails using mailgun http api.
 */
class DefaultMailgunService(implicit inj: Injector) extends Injectable with MailgunService {

  val key = inject[String] (identified by "mailgun.api.key")
  // we retrieve the domain from the smtp login since heroku mailgun does not give the domain alone
  val domain = inject[String] (identified by "mailgun.smtp.login").split("@").last
  val url = inject[String] (identified by "mailgun.api.url")
  val apiUrl = url.format(domain)

  private def requestHolder: WSRequestHolder = WS.url(apiUrl).withAuth("api", key, WSAuthScheme.BASIC)

  /**
   * Mailgun call will never reply with a failure, but with an forwarding that contains the updated status
   * @param from
   * @param to
   * @param content
   * @return mailgunId
   */
  def sendEmail(from: String, to: String, content: String): Future[String] = {

    val postData = Map(
      "from" -> Seq(emailSource(from)),
      "to" -> Seq(to),
      "subject" -> Seq("Sms forwarding"),
      "html" -> Seq(content)
    )

    val responseFuture: Future[WSResponse] = requestHolder.post(postData)

    val okResponse = responseFuture filter { _.status == Status.OK }
    okResponse flatMap { response => handleMailgunResponse(response.json)}
  }

  /**
   * Builds source email address
   * @param from
   * @return
   */
  def emailSource(from: String) = s"${from}@${domain}"

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
