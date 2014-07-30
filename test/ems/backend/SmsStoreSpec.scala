package ems.backend


import reactivemongo.bson.BSONObjectID
import com.github.nscala_time.time.Imports._
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoData, WithMongoTestData}
import ems.models.Sms


@RunWith(classOf[JUnitRunner])
class SmsStoreSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Sms store" should {

    "Be able to save a sms" in new WithMongoData(data) {
      val smsId = SmsStore.generateId
      val sms = smsList.head.copy(_id = smsId)
      val result = await(SmsStore.save(sms))

      result._id must beEqualTo(smsId)
    }

    "List user sms" in new WithMongoData(data) {
      val objectId = BSONObjectID.parse(userMongoId).get

      val result = await(SmsStore.listSms(objectId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(objectId)
    }
  }
}
