package ems.backend


import ems.backend.email.MailgunService
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence._
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import play.api.test.PlaySpecification
import scaldi.akka.AkkaInjectable
import scaldi.Module

import ems.models.{Sending, Message}
import ems.utils.{TestUtils, WithTestData}

import scala.util.Try


class ForwarderServiceActorSpec extends PlaySpecification with WithTestData with AkkaInjectable with TestUtils {

  implicit val timeout = Timeout(10.second)

  implicit val injector = new Module {
    bind[ForwarderServiceActor] toProvider new DefaultForwarderServiceActor

    binding identifiedBy "ems.backend.forwarding.DefaultForwarderServiceActor.sendToMailgunSleep" to 0
    bind[MessageStore] to mockMessageStore
    bind[MailgunService] to mockMailgunService
    bind[UserInfoStore] to mockUserInfoStore
    bind[UserStore] to mockUserStore
    bind[TwilioService] to mockTwilioService
    bind[UpdateService] to mockUpdateService
    bind[ActorSystem] to mockActorSystem
    bind[ExecutionContext] to mockExecutionContext
  }

  "Forwarder" should {

    "Forward sms to emails" in {
      implicit val system = inject[ActorSystem]
      val actorRef = injectActorRef[ForwarderServiceActor]
      val result = await((actorRef ? smsToEmailMessage).mapTo[Try[Message]])
      result must beSuccessfulTry.which(f => f.id == smsToEmailMessage.id and f.status == Sending)
    }

    "Forward emails to sms" in {
      implicit val system = inject[ActorSystem]
      val actorRef = injectActorRef[ForwarderServiceActor]
      val result = await((actorRef ? emailToSmsMessage).mapTo[Try[Message]])
      result must beSuccessfulTry.which(f => f.id == emailToSmsMessage.id and f.status == Sending)
    }

  }

}
