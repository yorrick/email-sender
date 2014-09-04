package ems.controllers.utils

import ems.backend.cms.{DefaultPrismicService, PrismicService}
import ems.utils.{TestUtils, WithTestData}
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.Action
import play.api.test._
import play.api.mvc.Results.Ok
import scaldi.{Injectable, Module}

import scala.concurrent.ExecutionContext


@RunWith(classOf[JUnitRunner])
class ContextActionSpec extends PlaySpecification with TestUtils with WithTestData with Injectable {
  sequential

  val someAction = Action {
    Ok("ok")
  }

  "ContextAction" should {

    "Not change the result" in {
      implicit val i = new Module {
        bind[PrismicService] to mockPrismicService
      }

      val response = ContextAction("welcome")(someAction).apply(FakeRequest())

      status(response) should beEqualTo(OK)
      contentAsString(response) must beEqualTo("ok")
    }

    "Still return same response even if prismic is down" in {
      implicit val i = new Module {
        bind[PrismicService] to new DefaultPrismicService
        // nobody listens to this port, so the call will fail
        binding identifiedBy "ems.controllers.MainController.prismic.api" to "http://localhost:19001/api"
        bind[ExecutionContext] to mockExecutionContext
      }
      val response = ContextAction("welcome")(someAction).apply(FakeRequest())

      status(response) should beEqualTo(OK)
      contentAsString(response) must beEqualTo("ok")
    }
  }
}
