package ems.backend


import ems.backend.updates.{UpdatesServiceActor, Disconnect, Connect}

import scala.concurrent.duration._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import akka.pattern.ask
import play.api.test.{PlaySpecification, WithApplication}
import scaldi.akka.AkkaInjectable

import ems.utils.{AppInjector, WithMongoTestData}


class UpdatesServiceActorSpec extends PlaySpecification with WithMongoTestData with AkkaInjectable with AppInjector {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(1.second)

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

  }

}
