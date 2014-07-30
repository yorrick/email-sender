package ems.utils


import reactivemongo.bson.BSONDocument

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable.Around
import org.specs2.specification.Scope
import reactivemongo.api.collections.default.BSONCollection
import play.api.{Logger, Application}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import play.api.test.{WithApplication, Helpers, FakeApplication}
import play.libs.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Context for mongodb based tests.
 * Data must be given as JsValue, this way serialization is handled by the test.
 * This context uses ReactiveMongoPlugin database connections to initialize data.
 * @param data
 * @param app
 */
abstract class WithMongoData(data: => Seq[(String, List[JsValue])] = Seq(),
                             override val app: FakeApplication = FakeApplication()) extends WithApplication(app)  {

  /**
   * Timeout for database statements
   */
  lazy val mongoStatementTimeout = 5.seconds

  override def around[T: AsResult](t: => T): Result = super.around {
    initMongo
    t
  }

  def initMongo {
    implicit val system = Akka.system
    implicit val db: reactivemongo.api.DB = ReactiveMongoPlugin.db

    val collectionResults: Seq[Future[Boolean]] = data map {
      case (collectionName, jsonDocuments) =>
        implicit val collection: BSONCollection = db(collectionName)
        dropCollection flatMap { _ => insertCollection(toBSONDocuments(jsonDocuments)) }
    }

    val results: Seq[Boolean] = Await.result(Future.sequence(collectionResults), mongoStatementTimeout)

    if (results exists {_ == false}) {
      throw new Exception("Could not initialize all mongo data")
    } else {
      Logger.debug("WithMongoData: initialized mongo data")
    }


  }

  /**
   * Converts play JsValue into reactive mongo BSONDocument
   * @param jsonDocuments
   * @return
   */
  def toBSONDocuments(jsonDocuments: List[JsValue]): List[BSONDocument] =
    jsonDocuments map {BSONFormats.BSONDocumentFormat.reads(_).get}

  /**
   * Drop all collection content
   * @param collection
   * @return
   */
  def dropCollection(implicit collection: BSONCollection): Future[Boolean] = collection.drop() filter { _ == true}

  /**
   * Inserts all bsonDocuments in given collection
   * @param collection
   * @param bsonDocuments
   * @return
   */
  def insertCollection(bsonDocuments: List[BSONDocument])(implicit collection: BSONCollection): Future[Boolean] = {
    collection.bulkInsert(Enumerator.enumerate(bsonDocuments)) map {
      case insertNumber: Int if insertNumber == bsonDocuments.length => true
      case _ => false
    }
  }
}
