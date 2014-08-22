package ems.backend


import scala.concurrent.duration._
import scala.concurrent.Future

import akka.pattern
import akka.actor.{ActorSystem, Actor}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger
import scaldi.{Injectable, Injector}

import ems.backend.utils.LogUtils
import ems.models._
import ems.backend.Twilio._


/**
 * An actor that forwards messages
 */
trait ForwarderServiceActor extends Actor

/**
 * Handles forwarding logic
 */
class DefaultForwarderServiceActor(implicit val inj: Injector) extends ForwarderServiceActor with LogUtils with Injectable with WithUpdateService {

  implicit val system: ActorSystem = inject[ActorSystem]

  val sendToMailgunSleep = inject[Int] (identified by "forwarder.mailgun.sleep")
  val forwardingStore = inject[ForwardingStore]
  val mailgun = inject[MailgunService]

  /**
   * Find user with incoming phone number
   * @param phoneNumber
   * @return
   */
  def findUserByPhoneNumber(phoneNumber: String): Future[User] = {
    for {
      userInfo <- UserInfoStore.findUserInfoByPhoneNumber(phoneNumber)
      user <- UserStore.findUserById(userInfo.id)
    } yield user
  }

  def findUserAndUserInfoByEmail(email: String): Future[UserInfo] = {
    for {
      user <- UserStore.findByEmail(email) andThen logResult("findByEmail")
      userInfo <- UserInfoStore.findUserInfoByUserId(user.id)
    } yield userInfo
  }

  def receive = {
    // sms -> email
    case forwarding: Forwarding if forwarding.smsToEmail =>
      Logger.debug(s"smsToEmail: ${forwarding.from}")

      val future = for {
        user <- findUserByPhoneNumber(forwarding.from)
        // add user and email to forwarding
        forwarding <- Future.successful(forwarding.withUserAndEmail(user))
        forwarding <- forwardingStore.save(forwarding) andThen notifyWebsockets
        mailgunId <- pattern.after(sendToMailgunSleep.second, Akka.system.scheduler)(mailgun.sendEmail(forwarding.from, user.main.email.get, forwarding.content))
        saved <- forwardingStore.updateMailgunIdById(forwarding.id, mailgunId)
        forwarding <- forwardingStore.updateStatusById(forwarding.id, Sending) andThen notifyWebsockets
      } yield forwarding

      val senderRef = sender()

      future onSuccess {
        case result @ _ =>
          senderRef ! result
      }

    // email -> sms
    case forwarding: Forwarding if forwarding.emailToSms =>
      Logger.debug(s"emailToSms: ${forwarding.from}")

      val future = for {
        userInfo <- findUserAndUserInfoByEmail(forwarding.from)
        // add user and phone number to forwarding
        forwarding <- Future.successful(forwarding.withUserInfoAndPhone(userInfo))
        forwarding <- forwardingStore.save(forwarding) andThen notifyWebsockets
        twilioId <- pattern.after(sendToMailgunSleep.second, Akka.system.scheduler)(sendSms(forwarding.to.get, forwarding.content))
        forwarding <- forwardingStore.updateStatusById(forwarding.id, Sent) andThen notifyWebsockets
      } yield forwarding

      val senderRef = sender()

      future onSuccess {
        case result @ _ =>
          senderRef ! result
      }

    case MailgunEvent(messageId, status) =>
      forwardingStore.updateStatusByMailgunId(messageId, status) andThen notifyWebsockets
  }
}
