/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services

import com.mongodb.DBObject
import com.mongodb.casbah.{MongoClient, MongoClientURI}

import scala.reflect.ClassTag

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

trait WithBSONConverters {

  def serialize[A](x: A)(f: (A) => DBObject)(implicit ctag: ClassTag[A]): DBObject = f(x)

  def deserialize[A](dbo: DBObject)(f: (DBObject) => A)(implicit ctag: ClassTag[A]): A = f(dbo)

}

trait WithMongoIndex {

  def ensureIndex(): Unit

}