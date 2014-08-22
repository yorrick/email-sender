package ems.backend


import scala.concurrent.duration._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import akka.testkit.TestActorRef
import akka.pattern.ask
import play.api.test.{PlaySpecification, WithApplication}

import ems.backend.WebsocketUpdatesService.{Connect, Disconnect}
import ems.utils.{AppInjector, WithMongoTestData}


class WebsocketUpdatesServiceSpec extends PlaySpecification with WithMongoTestData with AppInjector {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(1.second)

  "WebsocketUpdatesMaster" should {

    "Accept Connect and Disconnect messages" in new WithApplication() {
      implicit val injector = appInjector
      val actorRef = injectActorRef[ForwarderService]

      val actorRef = TestActorRef(new WebsocketUpdatesService)

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Disconnect unknow user should not raise an exception" in new WithApplication() {
      val actorRef = TestActorRef(new WebsocketUpdatesService)

      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Connect same user twice should work" in new WithApplication() {
      val actorRef = TestActorRef(new WebsocketUpdatesService)

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
    }

  }

}
