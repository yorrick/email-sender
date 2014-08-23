package ems.backend


import ems.backend.email.MailgunService
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence._
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import play.api.test.{FakeApplication, WithApplication, PlaySpecification}
import play.api.Play.current
import scaldi.akka.AkkaInjectable
import scaldi.Module

import ems.models.{Sending, Forwarding}
import ems.utils.{MockUtils, WithMongoTestData}

import scala.util.Try


class ForwarderServiceActorSpec extends PlaySpecification with WithMongoTestData with AkkaInjectable with MockUtils {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(10.second)

  implicit val injector = new Module {
    bind[ForwarderServiceActor] toProvider new DefaultForwarderServiceActor

    binding identifiedBy "forwarder.mailgun.sleep" to 0
    bind[ForwardingStore] to mockForwardingStore
    bind[MailgunService] to mockMailgunService
    bind[UserInfoStore] to mockUserInfoStore
    bind[UserStore] to mockUserStore
    bind[TwilioService] to mockTwilioService
    bind[UpdateService] to mockUpdateService
    bind[ActorSystem] to mockActorSystem
  }

  val app = new FakeApplication(withoutPlugins = Seq(
    "play.modules.rediscala.RedisPlugin",
    "play.modules.reactivemongo.ReactiveMongoPlugin"
  ))

  "Forwarder" should {

    "Forward sms to emails" in new WithApplication(app) {
      val actorRef = injectActorRef[ForwarderServiceActor]
      val result = await((actorRef ? smsToEmailForwarding).mapTo[Try[Forwarding]])
      result must beSuccessfulTry.which(f => f.id == smsToEmailForwarding.id and f.status == Sending)
    }

//    "Forward emails to sms" in new WithMongoApplication(data) {
//      val actorRef = injectActorRef[ForwarderServiceActor]
//      val result = await((actorRef ? emailToSmsForwarding).mapTo[Try[Forwarding]])
//      result must beSuccessfulTry.which(f => f.id == emailToSmsForwarding.id and f.status == Sending)
//    }
  }

}
