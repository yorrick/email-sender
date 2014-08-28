package ems.backend


import ems.backend.updates._
import ems.models.{MessageDisplay, Ping}
import scala.concurrent.duration._
import akka.actor.{ActorSystem, ActorRef}
import akka.util.Timeout
import akka.pattern.ask
import play.api.test.{PlaySpecification, WithApplication}
import scaldi.akka.AkkaInjectable
import ems.utils.{TestUtils, AppInjector, WithTestData}
import scala.util.Try


class UpdatesServiceActorSpec extends PlaySpecification with WithTestData with AkkaInjectable with AppInjector with TestUtils {

  implicit val timeout = Timeout(10.second)
  implicit val system = ActorSystem("TestActorSystem")

  "UpdatesServiceActorSpec" should {

    "Accept Connect and Disconnect messages" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Disconnect unknow user should not raise an exception" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Disconnect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(0)
    }

    "Connect same user twice should work" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
      await((actorRef ? Connect(user, actorRef)).mapTo[Map[String, ActorRef]]).size must beEqualTo(1)
    }

    "Accept messages" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? smsToEmailMessage).mapTo[Try[Long]]) must beSuccessfulTry
    }

    "Accept Signal messages" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      await((actorRef ? Ping).mapTo[Boolean]) must beTrue
    }

    "Accept MessageDisplay" in new WithApplication(noMongoApp) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[UpdatesServiceActor]

      val messageDisplay = MessageDisplay.fromMessage(smsToEmailMessage)

      await((actorRef ? messageDisplay).mapTo[Boolean]) must beTrue
    }

  }

}
