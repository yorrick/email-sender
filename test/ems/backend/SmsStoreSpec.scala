package ems.backend


import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._

import ems.utils.{WithMongoData, WithMongoTestData}
import reactivemongo.bson.BSONObjectID


@RunWith(classOf[JUnitRunner])
class SmsStoreSpec extends PlaySpecification with WithMongoTestData {
  sequential

  "Sms store" should {

    "List user sms" in new WithMongoData(data) {
      val objectId = BSONObjectID.parse(userMongoId).get

      val result = await(SmsStore.listSms(objectId))
      result.size must beEqualTo(1)
      result.head.userId must beEqualTo(objectId)
    }
  }
}
