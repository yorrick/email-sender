package ems.models


import reactivemongo.bson.BSONObjectID


/**
 * Base class for mongodb models that provides an "id" String value
 * @param _id
 */
abstract class MongoId(val _id: BSONObjectID) {
  @transient
  lazy val id = _id.stringify
}
