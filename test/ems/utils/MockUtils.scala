package ems.utils

import akka.actor.{Actor, ActorSystem}
import akka.testkit.TestActor.SetAutoPilot
import akka.testkit.{TestActor, TestActorRef}
import ems.backend.email.MailgunService
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.models.{Sending, ForwardingStatus, Forwarding}
import org.mockito.Matchers._

import scala.concurrent.Future

// to use matchers like anyInt()
import org.specs2.mock._
import ems.backend.persistence.{ForwardingStore, UserStore, UserInfoStore}

/**
 * Contains mocks that are used in the tests
 */
trait MockUtils extends Mockito { self: WithMongoTestData =>

  def mockForwardingStore = {
    val m = mock[ForwardingStore]
    m.save(anyObject[Forwarding]) returns Future.successful(smsToEmailForwarding)
    m.updateMailgunIdById(anyString, anyString) returns Future.successful(smsToEmailForwarding)
    m.updateStatusById(anyString, anyObject[ForwardingStatus]) returns Future.successful(smsToEmailForwarding.copy(status = Sending))

    m
  }

  def mockMailgunService = {
    val m = mock[MailgunService]
    m.sendEmail(anyString, anyString, anyString) returns Future.successful(smsToEmailForwarding.mailgunId)

    m
  }

  def mockUserInfoStore = {
    val m = mock[UserInfoStore]
    m.findUserInfoByPhoneNumber(anyString) returns Future.successful(userInfo)

    m
  }

  def mockUserStore = {
    val m = mock[UserStore]
    m.findUserById(anyString) returns Future.successful(user)

    m
  }

  def mockTwilioService = mock[TwilioService]

  def mockUpdateService = {
    val m = mock[UpdateService]
    implicit val system = mockActorSystem
    m.updatesServiceActor returns TestActorRef(new Actor { def receive = {case _ => }})

    m
  }

  def mockActorSystem = ActorSystem("TestActorSystem")
}
