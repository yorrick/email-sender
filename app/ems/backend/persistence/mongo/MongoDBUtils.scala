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
trait MongoDBUtils {
   def generateId = BSONObjectID.generate
 }
