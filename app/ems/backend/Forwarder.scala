package ems.backend


import ems.backend.utils.LogUtils
import scaldi.Injector
import scaldi.akka.AkkaInjectable

import scala.concurrent.duration._
import scala.concurrent.Future

import akka.pattern
import akka.actor.{Actor, Props}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.Logger

import ems.models._
import ems.backend.ForwardingStore._
import ems.backend.Mailgun._
import ems.backend.Twilio._
import ems.backend.WebsocketUpdatesMaster.notifyWebsockets


///**
// * Instance of the actor that handle forwarding routing
// */
//object Forwarder {
//  val forwarder = Akka.system.actorOf(Props[Forwarder], name="forwarder")
//}


/**
 * Handles forwarding logic
 */
class Forwarder(implicit inj: Injector) extends Actor with LogUtils with AkkaInjectable {

  val sendToMailgunSleep = current.configuration.getInt("forwarder.mailgun.sleep").getOrElse(2)

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
        forwarding <- save(forwarding) andThen notifyWebsockets
        mailgunId <- pattern.after(sendToMailgunSleep.second, Akka.system.scheduler)(sendEmail(forwarding.from, user.main.email.get, forwarding.content))
        saved <- ForwardingStore.updateMailgunIdById(forwarding.id, mailgunId)
        forwarding <- updateStatusById(forwarding.id, Sending) andThen notifyWebsockets
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
        forwarding <- save(forwarding) andThen notifyWebsockets
        twilioId <- pattern.after(sendToMailgunSleep.second, Akka.system.scheduler)(sendSms(forwarding.to.get, forwarding.content))
        forwarding <- updateStatusById(forwarding.id, Sent) andThen notifyWebsockets
      } yield forwarding

      val senderRef = sender()

      future onSuccess {
        case result @ _ =>
          senderRef ! result
      }

    case MailgunEvent(messageId, status) =>
      updateStatusByMailgunId(messageId, status) andThen notifyWebsockets
  }
}
