package ems.utils

import akka.actor.{Actor, ActorSystem}
import akka.testkit.TestActorRef
import ems.backend.email.MailgunService
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.backend.utils.RedisService
import ems.models.{Sending, ForwardingStatus, Forwarding}
import org.mockito.Matchers._
import play.api.test.FakeApplication
import redis.RedisClient

// to use matchers like anyInt()
import scala.concurrent.{ExecutionContext, Future}
import org.specs2.mock._
import ems.backend.persistence.{ForwardingStore, UserStore, UserInfoStore}
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Contains mocks that are used in the tests
 */
trait MockUtils extends Mockito { self: WithTestData =>

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

//    m.updateStatusById(anyObject[String], anyObject[ForwardingStatus]) answers { rawArgs =>
//      val args = rawArgs.asInstanceOf[Array[Any]]
//      val id = args(0).asInstanceOf[String]
//      val status = args(1).asInstanceOf[ForwardingStatus]
//      println(s"======================= $id $status")
//
//      Future.successful(forwardingMap.get(id).get.copy(status = status))
//    }

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
