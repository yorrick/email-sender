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
  def findUser(phoneNumber: String): Future[User] = {
    for {
      userInfo <- UserInfoStore.findUserInfoByPhoneNumber(phoneNumber)
      user <- UserStore.findUserById(userInfo.id)
    } yield user
  }

  def receive = {
    case forwarding: Forwarding =>

      for {
        user <- findUser(forwarding.from)
        forwarding <- Future.successful(forwarding.withUser(user._id))
        forwarding <- save(forwarding) andThen notifyWebsockets
        forwarding <- pattern.after(2.second, Akka.system.scheduler)(sendEmail(forwarding, user.main.email.get))
        forwarding <- updateStatusById(forwarding) andThen notifyWebsockets
      } yield forwarding

    case MailgunEvent(messageId, status) =>
      updateStatusByMailgunId(messageId, status) andThen notifyWebsockets
  }
}
