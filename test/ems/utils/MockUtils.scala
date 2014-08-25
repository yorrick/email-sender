package ems.utils

import _root_.securesocial.core.RuntimeEnvironment
import _root_.securesocial.core.authenticator.{Authenticator, AuthenticatorBuilder}
import _root_.securesocial.core.services.{AuthenticatorService}
import akka.actor.{Actor, ActorSystem}
import akka.testkit.TestActorRef
import com.github.nscala_time.time.Imports._
import ems.backend.WithGlobal
import ems.backend.email.MailgunService
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.models.{User, Sending, ForwardingStatus, Forwarding}
import ems.modules.WebModule
import org.mockito.Matchers._
import play.api.mvc.{Results, Result, RequestHeader, Cookie}
import play.api.test.FakeApplication
import play.mvc.Http.Context
import scaldi.play.ControllerInjector

import scala.concurrent.{ExecutionContext, Future}
import org.specs2.mock._
import ems.backend.persistence.{ForwardingStore, UserStore, UserInfoStore}
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Contains mocks that are used in the tests
 */
trait MockUtils extends Mockito { self: WithTestData =>

  /**
   * Cookie used for tests.
   * The value is not used as the AuthenticationStore is mocked
   * TODO find a way to use configuration (we need an app in context to be able to use CookieAuthenticator.cookieName)
   */
  lazy val cookie = Cookie("emailsenderid", "")

  /**
   * A runtime env that disables the authentication
   */
  def mockRuntimeEnvironment = new RuntimeEnvironment.Default[User] {
    override lazy val userService = mockUserStore
    override lazy val authenticatorService = new AuthenticatorService(mockAuthenticatorBuilder)
  }

  def mockAuthenticatorBuilder = new AuthenticatorBuilder[User] {
    val authenticator = mockAuthenticator

    def fromRequest(request: RequestHeader): Future[Option[Authenticator[User]]] = Future.successful(Some(authenticator))

    def fromUser(user: User): Future[Authenticator[User]] = Future.successful(authenticator)

    override val id: String = "mockAuthenticatorBuilder"
  }

  def mockAuthenticator = new Authenticator[User] with Results {
    override val id: String = "mockAuthenticator"

    override def touch: Future[Authenticator[User]] = Future.successful(this)

    override def updateUser(user: User): Future[Authenticator[User]] = Future.successful(this)

    override def discarding(result: Result): Future[Result] = Future.successful(result)

    override def discarding(javaContext: Context): Future[Unit] = Future.successful(Unit)

    override def touching(result: Result): Future[Result] = Future.successful(result)

    override def touching(javaContext: Context): Future[Unit] = Future.successful(Unit)

    override def isValid: Boolean = true

    override def starting(result: Result): Future[Result] = Future.successful(result)

    override val creationDate: DateTime = DateTime.lastDay
    override val user: User = self.user
    override val expirationDate: DateTime = DateTime.nextDay
    override val lastUsed: DateTime = DateTime.now
  }


  val mongoPluginClass = "play.modules.reactivemongo.ReactiveMongoPlugin"
  val redisPluginClass = "play.modules.rediscala.RedisPlugin"
  def noRedisApp = FakeApplication(withoutPlugins = Seq(redisPluginClass))
  def noMongoApp = FakeApplication(withoutPlugins = Seq(mongoPluginClass))
  def app = FakeApplication(withoutPlugins = Seq(mongoPluginClass, redisPluginClass))

  def mockForwardingStore = {
    val m = mock[ForwardingStore]

    m.save(anyObject[Forwarding]) answers {f => Future.successful(f.asInstanceOf[Forwarding])}

    m.updateMailgunIdById(anyString, anyString) answers { id =>
      Future.successful(forwardingMap.get(id.asInstanceOf[String]).get)
    }

    m.updateStatusById(anyObject[String], anyObject[ForwardingStatus]) answers { id =>
      Future.successful(forwardingMap.get(id.asInstanceOf[String]).get.copy(status = Sending))
    }

    m.listForwarding(anyString) returns Future.successful(forwardingList)

    m
  }

  def mockMailgunService = {
    val m = mock[MailgunService]

    m.sendEmail(anyString, anyString, anyString) returns Future.successful(smsToEmailForwarding.mailgunId)

    m
  }

  def mockUserInfoStore = {
    val m = mock[UserInfoStore]
    m.findUserInfoByPhoneNumber(anyString) returns Future.successful(userInfo)
    m.findUserInfoByUserId(anyString) returns Future.successful(userInfo)
    m.savePhoneNumber(anyString, anyString) returns Future.successful(userInfo)

    m
  }

  def mockUserStore = {
    val m = mock[UserStore]
    m.findUserById(anyString) returns Future.successful(user)
    m.findByEmail(anyString) returns Future.successful(user)

    m
  }

  def mockTwilioService = {
    val m = mock[TwilioService]
    m.sendSms(anyString, anyString) returns Future.successful("twilio_id")
    m.sendConfirmationSms(anyString) returns Future.successful("twilio_id")

    m
  }

  def mockUpdateService = {
    val m = mock[UpdateService]
    implicit val system = mockActorSystem
    m.updatesServiceActor returns TestActorRef(new Actor { def receive = {case _ => }})

    m
  }

  def mockActorSystem = ActorSystem("TestActorSystem")
  def mockExecutionContext: ExecutionContext = global

//  def mockRedisService = {
//    val m = mock[RedisService]
//
////    val client = mock[RedisClient]
////    client.publish(anyString, anyObject) returns Future.successful(1)
//
//    implicit val system = mockActorSystem
//    val client = spy(RedisClient())
//
//    m.client returns client
//
//    m
//  }

}
