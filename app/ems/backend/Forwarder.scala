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
import ems.backend.ForwardingStore._
import ems.backend.Mailgun._
import ems.backend.Twilio._
import ems.backend.WebsocketUpdatesMaster.notifyWebsockets


/**
 * Instance of the actor that handle forwarding routing
 */
object Forwarder {
  val forwarder = Akka.system.actorOf(Props[Forwarder], name="forwarder")
}


/**
 * Handles forwarding logic
 */
class Forwarder extends Actor {

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
      user <- UserStore.findByEmail(email)
      userInfo <- UserInfoStore.findUserInfoByUserId(user.id)
    } yield userInfo
  }

  def receive = {
    // sms -> email
    case forwarding: Forwarding if forwarding.smsToEmail =>

      for {
        user <- findUserByPhoneNumber(forwarding.from)
        // add user and email to forwarding
        forwarding <- Future.successful(forwarding.withUserAndEmail(user))
        forwarding <- save(forwarding) andThen notifyWebsockets
        mailgunId <- pattern.after(2.second, Akka.system.scheduler)(sendEmail(forwarding.from, user.main.email.get, forwarding.content))
        saved <- ForwardingStore.updateMailgunIdById(forwarding.id, mailgunId)
        forwarding <- updateStatusById(forwarding.id, Sending) andThen notifyWebsockets
      } yield forwarding

    // email -> sms
    case forwarding: Forwarding if forwarding.emailToSms =>

      for {
        userInfo <- findUserAndUserInfoByEmail(forwarding.from)
        // add user and phone number to forwarding
        forwarding <- Future.successful(forwarding.withUserInfoAndPhone(userInfo))
        forwarding <- save(forwarding) andThen notifyWebsockets
        smsSent <- pattern.after(2.second, Akka.system.scheduler)(sendSms(forwarding.to.get, forwarding.content))
        forwarding <- updateStatusById(forwarding.id, Sending) andThen notifyWebsockets
      } yield forwarding

    case MailgunEvent(messageId, status) =>
      updateStatusByMailgunId(messageId, status) andThen notifyWebsockets
  }
}
