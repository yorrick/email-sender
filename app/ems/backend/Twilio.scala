package ems.backend

import play.api.Play.current
import play.api.libs.ws.{WSAuthScheme, WS, WSRequestHolder, WSResponse}

import scala.concurrent.Future


/**
 * Contains utilities to connect to Twilio
 */
class Twilio {

//  curl -XPOST https://api.twilio.com/2010-04-01/Accounts/<SID>/Messages.json --data-urlencode "Body=Hello there" --data-urlencode "To=<+15140000000>" --data-urlencode "From=+14387938597"  -u '<SID>:<TOKEN>'

  val apiUrl = current.configuration.getString("twilio.api.url")
  val apiSid = current.configuration.getString("twilio.api.sid")
  val apiToken = current.configuration.getString("twilio.api.token")

  // TODO add credentials
  def requestHolderOption: Option[WSRequestHolder] = for {
    apiUrl <- apiUrl
    apiSid <- apiSid
    apiToken <- apiToken
  } yield WS.url(apiUrl)


  val responseFuture: Future[WSResponse] = requestHolderOption map { requestHolder =>
    requestHolder.post(postData)
  } getOrElse Future.failed(missingCredentials)

}
