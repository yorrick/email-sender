package ems.backend


import com.github.nscala_time.time.Imports._
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoData, WithMongoTestData}
import ems.models.{AckedByMailgun, SentToMailgun}


@RunWith(classOf[JUnitRunner])
class SmsStoreSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Sms store" should {

    "Be able to save a sms" in new WithMongoData(data) {
      val smsId = SmsStore.generateId
      val newSms = sms.copy(_id = smsId)
      val result = await(SmsStore.save(newSms))

      result._id must beEqualTo(smsId)
    }

    "Update status by id" in new WithMongoData(data) {
      val newSms = sms.copy(status = SentToMailgun)
      val result = await(SmsStore.updateStatusById(newSms))
      result.status must beEqualTo(SentToMailgun)
    }

    "Set status by mailgunId" in new WithMongoData(data) {
      val result = await(SmsStore.updateStatusByMailgunId(mailgunId, AckedByMailgun))
      result.status must beEqualTo(AckedByMailgun)
    }

    "Set sms mailgunId" in new WithMongoData(data) {
      val newMailgunId = "newMailgunId"
      val newSms = sms.copy(mailgunId = newMailgunId)
      val result = await(SmsStore.updateSmsMailgunId(newSms))
      result.mailgunId must beEqualTo(newMailgunId)
    }

    "List user sms" in new WithMongoData(data) {
      val result = await(SmsStore.listSms(userMongoId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(userMongoId)
    }
  }
}
