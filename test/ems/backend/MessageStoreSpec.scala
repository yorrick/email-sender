package ems.backend


import ems.backend.persistence.MessageStore
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.Injectable
import ems.utils.{TestUtils, AppInjector, WithMongoApplication, WithTestData}
import ems.models.{Sending, Sent}
import ems.backend.persistence.mongo.MongoDBUtils


@RunWith(classOf[JUnitRunner])
class MessageStoreSpec extends PlaySpecification with WithTestData with MongoDBUtils with Injectable with AppInjector with TestUtils {
  sequential

  "Message store" should {

    "Be able to save a message" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[MessageStore]

      val messageId = generateId
      val newMessage = smsToEmailMessage.copy(_id = messageId)
      val result = await(store.save(newMessage))

      result._id must beEqualTo(messageId)
    }

    "Update status by id" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[MessageStore]

      val result = await(store.updateStatusById(smsToEmailMessage.id, Sending))
      result.status must beEqualTo(Sending)
    }

    "Set status by mailgunId" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[MessageStore]

      val result = await(store.updateStatusByMailgunId(smsToEmailMailgunId, Sent))
      result.status must beEqualTo(Sent)
    }

    "Set message mailgunId" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[MessageStore]

      val newMailgunId = "newMailgunId"
      val result = await(store.updateMailgunIdById(smsToEmailMessage.id, newMailgunId))
      result.mailgunId must beEqualTo(newMailgunId)
    }

    "List user messages" in new WithMongoApplication(data, noRedisApp) {
      implicit val injector = appInjector
      val store = inject[MessageStore]

      val result = await(store.listMessage(userMongoId))
      result.size must beEqualTo(messageList.length)
      result.head.userId must beEqualTo(Some(userMongoId))
    }
  }
}
