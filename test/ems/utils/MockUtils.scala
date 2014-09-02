package ems.utils

import akka.actor.{Actor, ActorSystem}
import akka.testkit.TestActorRef
import ems.backend.cms.PrismicService
import ems.backend.email.MailgunService
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import ems.models.{Sending, MessageStatus, Message}
import io.prismic.DocumentLinkResolver
import io.prismic.Fragment.DocumentLink
import org.mockito.Matchers._
import scala.concurrent.{ExecutionContext, Future}
import org.specs2.mock._
import ems.backend.persistence.{MessageStore, UserStore, UserInfoStore}
import scala.concurrent.ExecutionContext.Implicits.global
import io.prismic


/**
 * Contains mocks that are used in the tests
 */
trait MockUtils extends Mockito { self: WithTestData =>

  def mockMessageStore = {
    val m = mock[MessageStore]

    m.save(anyObject[Message]) answers {f => Future.successful(f.asInstanceOf[Message])}

    m.updateMailgunIdById(anyString, anyString) answers { id =>
      Future.successful(messageMap.get(id.asInstanceOf[String]).get)
    }

    m.updateStatusById(anyObject[String], anyObject[MessageStatus]) answers { id =>
      Future.successful(messageMap.get(id.asInstanceOf[String]).get.copy(status = Sending))
    }

    m.listMessage(anyString) returns Future.successful(messageList)

    m
  }

  def mockMailgunService = {
    val m = mock[MailgunService]

    m.sendEmail(anyString, anyString, anyString) returns Future.successful(smsToEmailMessage.mailgunId)

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
    m.apiMainNumber returns "+15141234567"

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

  def mockPrismicService = {
    val m = mock[PrismicService]

    val doc = new prismic.Document("id", "type", "href", Seq[String](), Seq[String](), Nil, Map())
    m.getDocuments(anyObject()) returns Future.successful(Map("welcome" -> Seq(doc)))

    m.getLinkResolver returns Future.successful(new DocumentLinkResolver {
      override def apply(link: DocumentLink): String = ""
    })

    m
  }

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
