package ems.backend


import com.github.nscala_time.time.Imports._
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoData, WithMongoTestData}
import ems.models.{Sending, Sent}


@RunWith(classOf[JUnitRunner])
class ForwardingStoreSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Forwarding store" should {

    "Be able to save a forwarding" in new WithMongoData(data) {
      val forwardingId = ForwardingStore.generateId
      val newForwarding = forwarding.copy(_id = forwardingId)
      val result = await(ForwardingStore.save(newForwarding))

      result._id must beEqualTo(forwardingId)
    }

    "Update status by id" in new WithMongoData(data) {
      val result = await(ForwardingStore.updateStatusById(forwarding.id, Sending))
      result.status must beEqualTo(Sending)
    }

    "Set status by mailgunId" in new WithMongoData(data) {
      val result = await(ForwardingStore.updateStatusByMailgunId(mailgunId, Sent))
      result.status must beEqualTo(Sent)
    }

    "Set forwarding mailgunId" in new WithMongoData(data) {
      val newMailgunId = "newMailgunId"
      val result = await(ForwardingStore.updateMailgunIdById(forwarding.id, newMailgunId))
      result.mailgunId must beEqualTo(newMailgunId)
    }

    "List user forwarding" in new WithMongoData(data) {
      val result = await(ForwardingStore.listForwarding(userMongoId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(Some(userMongoId))
    }
  }
}
