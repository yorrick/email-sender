package ems.backend


import ems.backend.updates._
import ems.models.{ForwardingDisplay, Ping}
import scala.concurrent.duration._
import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import akka.pattern.ask
import play.api.test.{PlaySpecification, WithApplication}
import scaldi.akka.AkkaInjectable
import ems.utils.{MockUtils, AppInjector, WithTestData}
import scala.util.Try


class UpdatesServiceActorSpec extends PlaySpecification with WithTestData with AkkaInjectable with AppInjector with MockUtils {

  implicit val timeout = Timeout(10.second)
  implicit val system = ActorSystem("TestActorSystem")

//  implicit val injector = new Module {
//    bind[UpdatesServiceActor] to new WebsocketUpdatesServiceActor
//
//    bind[RedisService] to mockRedisService
//    bind[ExecutionContext] to mockExecutionContext
//  }

  "WebsocketUpdatesMaster" should {

    "Accept Connect and Disconnect messages" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Disconnect unknow user should not raise an exception" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Connect same user twice should work" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
    }

    "Accept Forwarding messages" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? smsToEmailForwarding).mapTo[Try[Long]]) must beSuccessfulTry.withValue(1)
    }

    "Accept Signal messages" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Ping).mapTo[Boolean]) must beTrue
    }

    "Accept ForwardingDisplay messages" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      val forwardingDisplay = ForwardingDisplay.fromForwarding(smsToEmailForwarding)

      await((actorRef ? forwardingDisplay).mapTo[Boolean]) must beTrue
    }

  }

}
