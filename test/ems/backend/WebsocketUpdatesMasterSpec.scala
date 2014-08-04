package ems.backend


import scala.concurrent.duration._

import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import akka.testkit.TestActorRef
import akka.pattern.ask
import play.api.test.{PlaySpecification, WithApplication}

import ems.backend.WebsocketUpdatesMaster.{Connect, Disconnect}
import ems.utils.WithMongoTestData


class WebsocketUpdatesMasterSpec extends PlaySpecification with WithMongoTestData {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(1.second)

  "WebsocketUpdatesMaster" should {

    "Accept Connect and Disconnect messages" in new WithApplication() {
      val actorRef = TestActorRef(new WebsocketUpdatesMaster)

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Disconnect unknow user should not raise an exception" in new WithApplication() {
      val actorRef = TestActorRef(new WebsocketUpdatesMaster)

      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Connect same user twice should work" in new WithApplication() {
      val actorRef = TestActorRef(new WebsocketUpdatesMaster)

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
    }

  }

}
