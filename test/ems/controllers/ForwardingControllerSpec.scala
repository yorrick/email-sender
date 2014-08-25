package ems.controllers


import ems.backend.persistence.{ForwardingStore, UserInfoStore}
import ems.models.User
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.http.{MimeTypes, HeaderNames}
import play.api.{Application, Logger}
import play.api.test._
import scaldi.play.ControllerInjector
import scaldi.{Module, Injectable}
import securesocial.core.RuntimeEnvironment
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import ems.utils.{AppInjector, MockUtils, WithTestData, WithMongoApplication}


@RunWith(classOf[JUnitRunner])
class ForwardingControllerSpec extends PlaySpecification with MockUtils with WithTestData with Injectable with AppInjector {
  sequential
//  isolated

  implicit val injector = new Module {
    bind[Application] to app

    bind[RuntimeEnvironment[User]] to mockRuntimeEnvironment
    bind[ExecutionContext] to mockExecutionContext
    bind[UserInfoStore] to mockUserInfoStore
    bind[ForwardingStore] to mockForwardingStore
  } :: new ControllerInjector


  step {
    Logger.info("Before class")
  }

  implicit val executionContext = global

  "Forwarding controller" should {

    "render the forwarding list page" in {
      val response = inject[ForwardingController].list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("Hello from sms")
      contentAsString(response) must contain ("Hello from email")
    }

    "block access to non logged users" in new WithApplication() {
      implicit val injector = appInjector
      val response = inject[ForwardingController].list(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.HTML))

      status(response) must equalTo(SEE_OTHER)
    }

  }

  step {
    Logger.info("After class")
  }
}
