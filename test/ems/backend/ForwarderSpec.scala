package ems.backend

import scala.concurrent.duration._


import play.api.mvc.{Action, Handler}
import play.api.mvc.Results._
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import play.api.test.{FakeApplication, PlaySpecification}

import ems.models.{Sending, Forwarding}
import ems.utils.{WithMongoServer, WithMongoTestData}

import scala.util.{Failure, Success}


class ForwarderSpec extends PlaySpecification with WithMongoTestData {

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
      val actorRef = TestActorRef(new Forwarder)
      val forwarding = smsToEmailForwarding.copy(_id = generateId)

      val result = await((actorRef ? forwarding).mapTo[Forwarding])
      result.id must beEqualTo(forwarding.id)
      result.status must beEqualTo(Sending)
    }
  }

}
