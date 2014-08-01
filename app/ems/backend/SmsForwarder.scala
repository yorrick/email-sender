package ems.backend

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.pattern
import akka.actor.{Actor, Props}
import com.github.nscala_time.time.Imports.DateTime
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._

import ems.models._
import ems.backend.SmsStore._
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

  /**
   * Creates the sms with the user and of course data from twilio
   * @param user
   * @return
   */
  def createSms(user: User, post: TwilioPost): Future[Sms] =
    Future {
      Sms(SmsStore.generateId, user._id, post.from, post.to, post.content, DateTime.now, SavedInMongo, "")
    }

  /**
   * Find user with incoming phone number
   * @param post
   * @return
   */
  def findUser(post: TwilioPost): Future[User] = {
    for {
      userInfo <- UserInfoStore.findUserInfoByPhoneNumber(post.from)
      user <- UserStore.findUserById(userInfo.id)
    } yield user
  }

  def receive = {
    case post: TwilioPost =>

      for {
        user <- findUser(post)
        sms <- createSms(user, post)
        sms <- save(sms) andThen notifyWebsockets
        sms <- pattern.after(2.second, Akka.system.scheduler)(sendEmail(sms, user.main.email.get))
        sms <- updateStatusById(sms) andThen notifyWebsockets
      } yield sms

    case MailgunEvent(messageId, DELIVERED) =>
      updateStatusByMailgunId(messageId, AckedByMailgun) andThen notifyWebsockets

    case MailgunEvent(messageId, _) =>
      updateStatusByMailgunId(messageId, FailedByMailgun) andThen notifyWebsockets
  }
}
