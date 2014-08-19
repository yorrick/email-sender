package ems.backend


import scala.concurrent.duration._

import play.api.mvc.{Action, Handler}
import play.api.mvc.Results._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import play.api.test.{FakeApplication, PlaySpecification}
import scaldi.akka.AkkaInjectable

import ems.models.{Sending, Forwarding}
import ems.utils.{AppInjector, WithMongoServer, WithMongoTestData}


class ForwarderSpec extends PlaySpecification with WithMongoTestData with AkkaInjectable with AppInjector {

  implicit val system = ActorSystem("TestActorSystem")
  implicit val timeout = Timeout(10.second)

  val resultMailgunId = "<xxxxxxxx@xxxx.mailgun.org>"

  val fakeMailgunResponse =
    s"""{"message": "Queued. Thank you.","id": "${resultMailgunId}"}"""

  /**
   * Intercepts all POST made to the application
   */
  val routes: PartialFunction[(String, String), Handler] = {
    case ("POST", _: String) =>
      Action { Ok(fakeMailgunResponse) }
  }

  val app = FakeApplication(withRoutes = routes)


  "Forwarder" should {

    "Forward sms to emails" in new WithMongoServer(data, app) {
      implicit val injector = appInjector
      val actorRef = injectActorRef[ForwarderService]

      val forwarding = smsToEmailForwarding.copy(_id = generateId)

      val result = await((actorRef ? forwarding).mapTo[Forwarding])
      result.id must beEqualTo(forwarding.id)
      result.status must beEqualTo(Sending)
    }
  }

}
