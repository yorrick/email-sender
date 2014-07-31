package ems.backend

import scala.concurrent.Future

import reactivemongo.api._
import reactivemongo.bson.BSONObjectID

import play.api.Play.current
import play.api.libs.json._
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.json.BSONFormats._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import ems.models._


/**
 * Common utilities for mongodb stores
 */
trait MongoDBStore {

  def collectionName: String
  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
  def collection: JSONCollection = db.collection[JSONCollection](collectionName)
  def generateId = BSONObjectID.generate

  /**
   * Finds a single result if there is only one, or None
   * @param cursor
   * @tparam T
   * @return
   */
  def findSingle[T](cursor: Cursor[T]): Future[Option[T]] = cursor.collect[List]() map {
    case element :: Nil => Some(element)
    case _ => None
  }

}
