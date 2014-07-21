package ems.backend

import scala.concurrent.duration._

import akka.pattern
import akka.actor.{Actor, Props}
import com.github.nscala_time.time.Imports.DateTime
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._
import ems.backend.MongoDB._
import ems.backend.Mailgun._
import ems.backend.WebsocketUpdatesMaster.notifyWebsockets


/**
 * Instance of the actor that handle sms forwarding
 */
object SmsForwarder {
  val smsForwarder = Akka.system.actorOf(Props[SmsForwarder], name="smsForwarder")
}


/**
 * Handles sms forwarding logic
 */
class SmsForwarder extends Actor {

  def receive = {
    case post: TwilioPost =>
      val sms = Sms(MongoDB.generateId, post.from, post.to, post.content, DateTime.now, SavedInMongo, "")

      for {
        sms <- save(sms) andThen notifyWebsockets
        sms <- pattern.after(2.second, Akka.system.scheduler)(sendEmail(sms))
        sms <- updateStatusById(sms) andThen notifyWebsockets
      } yield sms

    case MailgunEvent(messageId, DELIVERED) =>
      setStatusByMailgunId(messageId, AckedByMailgun) andThen notifyWebsockets

    case MailgunEvent(messageId, _) =>
      setStatusByMailgunId(messageId, FailedByMailgun) andThen notifyWebsockets
  }
}
