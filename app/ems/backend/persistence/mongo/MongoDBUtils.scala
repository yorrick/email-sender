package ems.backend.persistence.mongo

import reactivemongo.bson.BSONObjectID


/**
  * Common utilities for mongodb stores
  */
trait MongoDBUtils {
   def generateId = BSONObjectID.generate
 }
