package ems.backend.persistence.mongo

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future


/**
 * Common utilities for mongodb stores
 */
trait MongoDBStore extends MongoDBUtils {

  def collectionName: String
  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection](collectionName)

  /**
   * Finds a single result if there is only one, or None
   * @param cursor
   * @tparam T
   * @return
   */
  def findSingle[T](cursor: Cursor[T]): Future[T] = cursor.collect[List]() flatMap {
    case element :: Nil => Future.successful(element)
    case Nil => Future.failed(new Exception("Could not find any element"))
    case _ => Future.failed(new Exception("More than one element matched query"))
  }

  /**
   * Parse a bson object id
   * @param id
   * @return
   */
  def toBSONObjectId(id: String): Future[BSONObjectID] = Future {
    BSONObjectID.parse(id).get
  }

}
