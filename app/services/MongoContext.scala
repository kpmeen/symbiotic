/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services

import com.mongodb.casbah.{MongoClientURI, MongoClient}

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