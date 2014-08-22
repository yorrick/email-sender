package ems.utils


import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.BSONCommandError
import play.api.libs.iteratee.Enumerator
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.BSONFormats
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import play.api.{Application, Logger}
import play.api.libs.concurrent.Execution.Implicits._


/**
 * Utility functions for mongo based tests.
 * Data must be given as JsValue, this way serialization is handled by the test.
 * This context uses ReactiveMongoPlugin database connections to initialize data.
 */
trait WithMongoTestUtils {

  val data: Seq[(String, List[JsValue])]
  implicit val app: Application

  /**
   * Timeout for database statements
   */
  lazy val mongoStatementTimeout = 5.seconds

  def initMongo {
    implicit val system = Akka.system
    implicit val db: reactivemongo.api.DB = ReactiveMongoPlugin.db

    Logger.debug("WithMongoData: initializing mongo data")

    val collectionResults: Seq[Future[Boolean]] = data map {
      case (collectionName, jsonDocuments) =>
        implicit val collection: BSONCollection = db(collectionName)
        dropCollection flatMap { _ => insertCollection(toBSONDocuments(jsonDocuments)) }
    }

    val results: Seq[Boolean] = Await.result(Future.sequence(collectionResults), mongoStatementTimeout)

    if (results exists {_ == false}) {
      throw new Exception("WithMongoData: Could not initialize all mongo data")
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
   * Since the collection may not exist, we ignore errors coming from drops
   * @param collection
   * @return
   */
  def dropCollection(implicit collection: BSONCollection): Future[Boolean] = collection.drop() recover { case e: BSONCommandError => true}

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
