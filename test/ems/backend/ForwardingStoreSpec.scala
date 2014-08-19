package ems.backend


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.Injectable
import scaldi.play.ScaldiSupport

import ems.utils.{AppInjector, WithMongoApplication, WithMongoTestData}
import ems.models.{Sending, Sent}
import ems.backend.mongo.MongoDBUtils


@RunWith(classOf[JUnitRunner])
class ForwardingStoreSpec extends PlaySpecification with WithMongoTestData with MongoDBUtils with Injectable with AppInjector {
  sequential

  "Forwarding store" should {

    "Be able to save a forwarding" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[ForwardingStore]

      val forwardingId = generateId
      val newForwarding = smsToEmailForwarding.copy(_id = forwardingId)
      val result = await(store.save(newForwarding))

      result._id must beEqualTo(forwardingId)
    }

    "Update status by id" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[ForwardingStore]

      val result = await(store.updateStatusById(smsToEmailForwarding.id, Sending))
      result.status must beEqualTo(Sending)
    }

    "Set status by mailgunId" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[ForwardingStore]

      val result = await(store.updateStatusByMailgunId(mailgunId, Sent))
      result.status must beEqualTo(Sent)
    }

    "Set forwarding mailgunId" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[ForwardingStore]

      val newMailgunId = "newMailgunId"
      val result = await(store.updateMailgunIdById(smsToEmailForwarding.id, newMailgunId))
      result.mailgunId must beEqualTo(newMailgunId)
    }

    "List user forwarding" in new WithMongoApplication(data) {
      implicit val injector = appInjector
      val store = inject[ForwardingStore]

      val result = await(store.listForwarding(userMongoId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(Some(userMongoId))
    }
  }
}
