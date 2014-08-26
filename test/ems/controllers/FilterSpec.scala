package ems.controllers

import ems.controllers.utils.HttpsOnlyFilter
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.{RequestHeader, Result, Filter}
import play.api.mvc.Results.Ok
import play.api.test._
import scaldi.{Injectable, Module}
import scala.concurrent.Future


@RunWith(classOf[JUnitRunner])
class FilterSpec extends PlaySpecification with Injectable {
  sequential

  val httpHeader = "x-forwarded-proto"

  def injector(enabled: Boolean) = new Module {
    bind[Filter] to new HttpsOnlyFilter
    binding identifiedBy "ems.controllers.utils.HttpsOnlyFilter.enabled" to enabled
    binding identifiedBy "ems.controllers.utils.HttpsOnlyFilter.httpHeader" to httpHeader
  }

  val action: RequestHeader => Future[Result] = {_ => Future.successful(Ok("hello"))}

  "HttpsOnlyFilter" should {

    "Redirect to https if connection is not secured" in {
      implicit val inj = injector(true)
      val request = FakeRequest().withHeaders(httpHeader -> "http")
      val result: Future[Result] = inject[Filter].apply(action)(request)

      status(result) must equalTo(MOVED_PERMANENTLY)
    }

    "Execute action if connection is secured" in {
      implicit val inj = injector(true)
      val request = FakeRequest().withHeaders(httpHeader -> "https")
      val result: Future[Result] = inject[Filter].apply(action)(request)

      status(result) must equalTo(OK)
    }

    "Execute action if no header is present" in {
      implicit val inj = injector(true)
      val result: Future[Result] = inject[Filter].apply(action)(FakeRequest())

      status(result) must equalTo(OK)
    }

    "Execute action if disabled" in {
      implicit val inj = injector(false)
      val request = FakeRequest().withHeaders(httpHeader -> "http")
      val result: Future[Result] = inject[Filter].apply(action)(request)

      status(result) must equalTo(OK)
    }

  }

}
