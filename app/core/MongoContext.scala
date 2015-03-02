/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core

import com.mongodb.DBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}

object MongoContext {

  val uri = MongoClientURI("mongodb://localhost:27017/")

  val client = MongoClient(uri)

  val defaultDb = client("copr8")

}

trait WithMongo {

  val collectionName: String

  val client = MongoContext.client

  val db = MongoContext.defaultDb

  lazy val collection = db(collectionName)

}

trait WithBSONConverters[T] {

  implicit def toBSON(x: T): DBObject

  implicit def fromBSON(dbo: DBObject): T

}

trait WithMongoIndex {

  def ensureIndex(): Unit

}