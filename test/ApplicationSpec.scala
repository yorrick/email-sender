import scala.concurrent.Future
import scala.reflect.ClassTag

import com.github.nscala_time.time.Imports.DateTime
import org.junit.runner.RunWith
import org.specs2.mock.Mockito
import org.specs2.mutable._
import org.specs2.runner._
import reactivemongo.bson.BSONObjectID
import securesocial.core.{AuthenticationMethod, BasicProfile}
import securesocial.core.authenticator._
import securesocial.core.services.{SaveMode, AuthenticatorService}

import play.api.libs.json.JsValue
import play.api.Logger
import play.api.test._
import play.api.mvc.Cookie

import ems.models.{Sms, SavedInMongo, User}
import ems.controllers.{SmsController, Application}
import ems.backend.utils.WithControllerUtils
import ems.backend.Mailgun._


/**
 * A store that always returns the same User
 */
class MockAuthenticatorStore(val user: User) extends AuthenticatorStore[CookieAuthenticator[User]] {

  val authenticator: CookieAuthenticator[User] = new CookieAuthenticator(
    "emailsenderid", user, DateTime.nextDay, DateTime.now, DateTime.lastDay, this)

  def find(id: String)(implicit ct: ClassTag[CookieAuthenticator[User]]): Future[Option[CookieAuthenticator[User]]] = {
    Future.successful(Some(authenticator))
  }

  def save(authenticator: CookieAuthenticator[User], timeoutInSeconds: Int): Future[CookieAuthenticator[User]] = {
    Future.successful(authenticator)
  }

  def delete(id: String): Future[Unit] = Future.successful(Unit)
}


///**
// * A user service that always returns the same User
// */
//class MockUserService extends ExternalUserService[User] {
//  def find(providerId: String, userId: String): Future[Option[BasicProfile]] = {
//
//  }
//
//  def findUser(providerId: String, userId: String): Future[Option[User]] = {
//
//  }
//
//  def findByEmailAndProvider(email: String, providerId: String): Future[Option[BasicProfile]] = {
//
//  }
//
//  def save(user: BasicProfile, mode: SaveMode): Future[User] = {
//
//  }
//
//  def link(current: User, to: BasicProfile): Future[User] = {
//    Future.failed(new Exception("not implemented"))
//  }
//
//}


@RunWith(classOf[JUnitRunner])
class SmsSpec extends PlaySpecification with WithControllerUtils with Mockito {
  sequential
//  isolated

  val user = User(BasicProfile(
    "providerId",
    "userid-12345",
    Some("Paul"),
    Some("Watson"),
    Some("Paul Watson"),
    Some("paul.watson@foobar.com"),
    None,
    AuthenticationMethod.OAuth2,
    None,
    None,
    None
  ))

  val runtimeEnv = new Global.EMSRuntimeEnvironment {
    override lazy val authenticatorService = new AuthenticatorService(
      // TODO mock fromRequest?
      new CookieAuthenticatorBuilder[User](new MockAuthenticatorStore(user), idGenerator)
    )
  }

  def createController[A] = getControllerInstance[A, User](runtimeEnv)_
  val smsController = createController(classOf[SmsController]).get
  val applicationController = createController(classOf[Application]).get

  step {
    Logger.info("Before class")
  }

  val smsId = "53cd93ce93d970b47bea76fd"
  val smsList = List(Sms(BSONObjectID.parse(smsId).get, "11111111", "222222222", "some text", DateTime.now, SavedInMongo, ""))
  val bsonList: List[JsValue] = smsList map {Sms.smsFormat.writes(_)}
  val data = ("smslist", bsonList)

  "Sms controller" should {

    "render the sms list page" in new InitDB(data) {
      // TODO fetch cookie name in config?
      val response = smsController.list(FakeRequest().withCookies(Cookie("emailsenderid", "we dont care")))

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("some text")
    }

  }

  "Twilio controller" should {

    "Accept post data for sms" in new InitDB(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val postResponse = ems.controllers.TwilioController.sms(request)
      status(postResponse) must equalTo(OK)
      println(contentAsString(postResponse))
      contentAsString(postResponse) must contain("Message")

      val listResponse = smsController.list(FakeRequest().withCookies(Cookie("emailsenderid", "we dont care")))
      status(listResponse) must equalTo(OK)
      contentType(listResponse) must beSome.which(_ == "text/html")
      contentAsString(listResponse) must contain ("some text")
      contentAsString(listResponse) must contain ("hello toto")
    }

  }

  "Mailgun controller" should {

    "Accept post data for delivery ack" in new InitDB(data) {
      val request = FakeRequest(POST, "").withFormUrlEncodedBody(
        "Message-Id" -> smsId,
        "event" -> DELIVERED
      )

      val postResponse = ems.controllers.MailgunController.success(request)
      status(postResponse) must equalTo(OK)
      contentAsString(postResponse) must equalTo("")
    }

  }

  "Main module" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = applicationController.index(FakeRequest().withCookies(Cookie("emailsenderid", "we dont care")))

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Sms forwardings")
    }
  }

  step {
    Logger.info("After class")
  }
}
