package ems.backend.forwarding

import akka.actor.ActorSystem
import akka.pattern
import ems.backend.email.MailgunService
import ems.backend.persistence.{ForwardingStore, UserInfoStore, UserStore}
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.backend.utils.LogUtils
import ems.models._
import play.api.Logger
//import play.api.Play.current
//import play.api.libs.concurrent.Akka
//import play.api.libs.concurrent.Execution.Implicits._
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

  val sendToMailgunSleep = inject[Int] (identified by "forwarder.mailgun.sleep")
  val forwardingStore = inject[ForwardingStore]
  val mailgun = inject[MailgunService]
  val userInfoStore = inject[UserInfoStore]
  val userStore = inject[UserStore]
  val twilioService = inject[TwilioService]
  val updatesServiceActor = inject[UpdateService].updatesServiceActor

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Forwarding], Unit] = {
    case Success(forwarding) =>
      updatesServiceActor ! forwarding
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
    case forwarding: Forwarding if forwarding.smsToEmail =>
      Logger.debug(s"smsToEmail: ${forwarding.from}")

      val future = for {
        user <- findUserByPhoneNumber(forwarding.from) andThen logResult("findUserByPhoneNumber")
        // add user and email to forwarding
        forwarding <- Future.successful(forwarding.withUserAndEmail(user)) andThen logResult("withUserAndEmail")
        forwarding <- forwardingStore.save(forwarding) andThen notifyWebsockets
        mailgunId <- pattern.after(sendToMailgunSleep.second, system.scheduler)(mailgun.sendEmail(forwarding.from, user.main.email.get, forwarding.content))
        saved <- forwardingStore.updateMailgunIdById(forwarding.id, mailgunId) andThen logResult("updateMailgunIdById")
        forwarding <- forwardingStore.updateStatusById(forwarding.id, Sending) andThen notifyWebsockets andThen logResult("updateStatusById")
      } yield forwarding

      val senderRef = sender()

      // send back whatever result we got
      future onComplete {
        case result @ _ => senderRef ! result
      }

    // email -> sms
    case forwarding: Forwarding if forwarding.emailToSms =>
      Logger.debug(s"emailToSms: ${forwarding.from}")

      val future = for {
        userInfo <- findUserAndUserInfoByEmail(forwarding.from)
        // add user and phone number to forwarding
        forwarding <- Future.successful(forwarding.withUserInfoAndPhone(userInfo))
        forwarding <- forwardingStore.save(forwarding) andThen notifyWebsockets
        twilioId <- pattern.after(sendToMailgunSleep.second, system.scheduler)(twilioService.sendSms(forwarding.to.get, forwarding.content))
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
