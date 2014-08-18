package ems.backend


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoApplication, WithMongoTestData}
import ems.models.{Sending, Sent}


@RunWith(classOf[JUnitRunner])
class ForwardingStoreSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Forwarding store" should {

    "Be able to save a forwarding" in new WithMongoApplication(data) {
      val forwardingId = ForwardingStore.generateId
      val newForwarding = smsToEmailForwarding.copy(_id = forwardingId)
      val result = await(ForwardingStore.save(newForwarding))

      result._id must beEqualTo(forwardingId)
    }

    "Update status by id" in new WithMongoApplication(data) {
      val result = await(ForwardingStore.updateStatusById(smsToEmailForwarding.id, Sending))
      result.status must beEqualTo(Sending)
    }

    "Set status by mailgunId" in new WithMongoApplication(data) {
      val result = await(ForwardingStore.updateStatusByMailgunId(mailgunId, Sent))
      result.status must beEqualTo(Sent)
    }

    "Set forwarding mailgunId" in new WithMongoApplication(data) {
      val newMailgunId = "newMailgunId"
      val result = await(ForwardingStore.updateMailgunIdById(smsToEmailForwarding.id, newMailgunId))
      result.mailgunId must beEqualTo(newMailgunId)
    }

    "List user forwarding" in new WithMongoApplication(data) {
      val result = await(ForwardingStore.listForwarding(userMongoId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(Some(userMongoId))
    }
  }
}
