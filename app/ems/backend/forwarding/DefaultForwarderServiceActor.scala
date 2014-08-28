package ems.backend.forwarding

import akka.actor.ActorSystem
import akka.pattern
import ems.backend.email.MailgunService
import ems.backend.persistence.{MessageStore, UserInfoStore, UserStore}
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.backend.utils.LogUtils
import ems.models._
import play.api.Logger
import scaldi.{Injectable, Injector}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Success, Try}


/**
 * Handles forwarding logic
 */
class DefaultForwarderServiceActor(implicit val inj: Injector) extends ForwarderServiceActor with LogUtils with Injectable {

  implicit val system = inject[ActorSystem]
  implicit val executionContext = inject[ExecutionContext]

  val sendToMailgunSleep = inject[Int] (identified by "ems.backend.forwarding.DefaultForwarderServiceActor.sendToMailgunSleep")
  val messageStore = inject[MessageStore]
  val mailgun = inject[MailgunService]
  val userInfoStore = inject[UserInfoStore]
  val userStore = inject[UserStore]
  val twilioService = inject[TwilioService]
  val updatesServiceActor = inject[UpdateService].updatesServiceActor

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Message], Unit] = {
    case Success(message) =>
      updatesServiceActor ! message
  }

  /**
   * Find user with incoming phone number
   * @param phoneNumber
   * @return
   */
  def findUserByPhoneNumber(phoneNumber: String): Future[User] = {
    for {
      userInfo <- userInfoStore.findUserInfoByPhoneNumber(phoneNumber)
      user <- userStore.findUserById(userInfo.id)
    } yield user
  }

  def findUserAndUserInfoByEmail(email: String): Future[UserInfo] = {
    for {
      user <- userStore.findByEmail(email) andThen logResult("findByEmail")
      userInfo <- userInfoStore.findUserInfoByUserId(user.id)
    } yield userInfo
  }

  def receive = {
    // sms -> email
    case message: Message if message.smsToEmail =>
      Logger.debug(s"smsToEmail: ${message.from}")

      val future = for {
        user <- findUserByPhoneNumber(message.from) andThen logResult("findUserByPhoneNumber")
        // add user and email to message
        message <- Future.successful(message.withUserAndEmail(user)) andThen logResult("withUserAndEmail")
        message <- messageStore.save(message) andThen notifyWebsockets
        mailgunId <- pattern.after(sendToMailgunSleep.second, system.scheduler)(mailgun.sendEmail(message.from, user.main.email.get, message.content))
        saved <- messageStore.updateMailgunIdById(message.id, mailgunId) andThen logResult("updateMailgunIdById")
        message <- messageStore.updateStatusById(message.id, Sending) andThen notifyWebsockets andThen logResult("updateStatusById")
      } yield message

      val senderRef = sender()

      // send back whatever result we got
      future onComplete {
        case result @ _ => senderRef ! result
      }

    // email -> sms
    case message: Message if message.emailToSms =>
      Logger.debug(s"emailToSms: ${message.from}")

      val future = for {
        userInfo <- findUserAndUserInfoByEmail(message.from)
        // add user and phone number to message
        message <- Future.successful(message.withUserInfoAndPhone(userInfo))
        message <- messageStore.save(message) andThen notifyWebsockets
        twilioId <- pattern.after(sendToMailgunSleep.second, system.scheduler)(twilioService.sendSms(message.to.get, message.content))
        message <- messageStore.updateStatusById(message.id, Sent) andThen notifyWebsockets
      } yield message

      val senderRef = sender()

      // send back whatever result we got
      future onComplete {
        case result @ _ => senderRef ! result
      }

    case MailgunEvent(messageId, status) =>
      messageStore.updateStatusByMailgunId(messageId, status) andThen notifyWebsockets
  }
}
