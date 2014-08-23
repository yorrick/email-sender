package ems.backend


import ems.backend.email.MailgunService
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence._
import ems.backend.sms.TwilioService
import ems.backend.updates.{WebsocketUpdatesServiceActor, UpdatesServiceActor, UpdateService}
import ems.backend.utils.RedisService
import play.api.libs.concurrent.Akka
import org.specs2.mock._

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.mvc.{Action, Handler}
import play.api.mvc.Results._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import play.api.test.{FakeApplication, PlaySpecification}
import play.api.Play.current
import scaldi.akka.AkkaInjectable
import scaldi.Module

import ems.models.{Sending, Forwarding}
import ems.utils.{WithMongoApplication, MockUtils, WithMongoServer, WithMongoTestData}

import scala.util.Try


class ForwarderServiceActorSpec extends PlaySpecification with WithMongoTestData with AkkaInjectable with MockUtils {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(10.second)

//  val resultMailgunId = "<xxxxxxxx@xxxx.mailgun.org>"
//
//  val fakeMailgunResponse =
//    s"""{"message": "Queued. Thank you.","id": "${resultMailgunId}"}"""

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

  "Forwarder" should {

    "Forward sms to emails" in new WithMongoApplication(data) {
      val actorRef = injectActorRef[ForwarderServiceActor]
      val result = await((actorRef ? smsToEmailForwarding).mapTo[Try[Forwarding]])
      result must beSuccessfulTry.which(f => f.id == smsToEmailForwarding.id and f.status == Sending)
    }
  }

}
