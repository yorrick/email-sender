package ems.backend


import akka.actor.ActorSystem
import ems.backend.email.MailgunService
import ems.backend.forwarding.{DefaultForwarderServiceActor, ForwarderServiceActor}
import ems.backend.persistence.{UserStore, UserInfoStore, ForwardingStore}
import ems.backend.sms.TwilioService
import ems.backend.updates.UpdateService
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.test._
import scaldi.{Module, Injectable}
import scaldi.play.ScaldiSupport

import ems.utils.{AppInjector, WithMongoApplication, WithTestData}
import ems.models.{Sending, Sent}
import ems.backend.persistence.mongo.MongoDBUtils

import scala.concurrent.ExecutionContext


@RunWith(classOf[JUnitRunner])
class ForwardingStoreSpec extends PlaySpecification with WithTestData with MongoDBUtils with Injectable with AppInjector {
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

      val result = await(store.updateStatusByMailgunId(smsToEmailMailgunId, Sent))
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
      result.size must beEqualTo(forwardingList.length)
      result.head.userId must beEqualTo(Some(userMongoId))
    }
  }
}
