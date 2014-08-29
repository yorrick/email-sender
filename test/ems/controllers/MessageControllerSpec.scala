package ems.controllers


import ems.backend.persistence.{MessageStore, UserInfoStore}
import ems.models.User
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.http.{MimeTypes, HeaderNames}
import play.api.{Logger}
import play.api.test._
import scaldi.{Module, Injectable}
import securesocial.core.RuntimeEnvironment
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import ems.utils.{AppInjector, TestUtils, WithTestData}


@RunWith(classOf[JUnitRunner])
class MessageControllerSpec extends PlaySpecification with TestUtils with WithTestData with Injectable with AppInjector {
  sequential
//  isolated

  def testInjector = new Module {
    bind[RuntimeEnvironment[User]] to mockRuntimeEnvironment
    bind[ExecutionContext] to mockExecutionContext
    bind[UserInfoStore] to mockUserInfoStore
    bind[MessageStore] to mockMessageStore
  }


  step {
    Logger.info("Before class")
  }

  implicit val executionContext = global

  "Message controller" should {

    "render the message list page" in new WithApplication(app) {
      implicit val i = testInjector :: appInjector
      val response = inject[MessageController].list(FakeRequest().withCookies(cookie))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("Hello from sms")
      contentAsString(response) must contain ("Hello from email")
    }

    "block access to non logged users" in new WithApplication() {
      implicit val injector = appInjector
      val response = inject[MessageController].list(FakeRequest().withHeaders(HeaderNames.ACCEPT -> MimeTypes.HTML))

      status(response) must equalTo(SEE_OTHER)
    }

  }

  step {
    Logger.info("After class")
  }
}
