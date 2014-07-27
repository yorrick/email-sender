package ems.utils


import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable.Around
import org.specs2.specification.Scope
import play.api.Application
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.test.{Helpers, FakeApplication}
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats
import reactivemongo.api.collections.default.BSONCollection

import scala.concurrent.{Await, Future}


/**
 * This context allows to initialize mongo db collections
 * @param data
 */
class InitDB(val data: Tuple2[String, List[JsValue]]*) extends Around with Scope {

  import scala.concurrent.duration._
  import play.api.libs.concurrent.Execution.Implicits._

  implicit lazy val app = FakeApplication()
  lazy val db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  lazy val timeout = 20.seconds

  override def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively {
      initDB(app)
      try {
        t
      } finally {
        //        teardown(app)
      }
    })
  }

  def initDB(app: Application) {

    data foreach {
      case (collectionName, jsonDocuments) =>
        val bsonDocuments = jsonDocuments map {BSONFormats.BSONDocumentFormat.reads(_).get}

        val collection: BSONCollection = db(collectionName)

        // recover in case the collection does not exist
        val dropFuture: Future[Boolean] = collection.drop() recover {
          case t @ _ => false
        }
        Await.result(dropFuture, timeout)

        val insertFuture: Future[Int] = collection.bulkInsert(Enumerator.enumerate(bsonDocuments)).mapTo[Int] recover {
          case t @ _ => 0
        }

        val inserted = Await.result(insertFuture, timeout)
        if (inserted < bsonDocuments.length) {
          throw new Exception(s"Could not insert all initial data of collection $bsonDocuments: inserted $inserted instead of ${bsonDocuments.length}")
        }
    }

  }
}
