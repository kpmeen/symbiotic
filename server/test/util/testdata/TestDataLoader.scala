package util.testdata

/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */

import core.security.authentication.Crypto
import models.party.User
import play.api.libs.json.{Json, Reads}
import repository.mongodb.party.MongoDBUserRepository

import scala.io.Source
import scala.reflect.ClassTag

object TestDataLoader extends App {

  val userRepo = new MongoDBUserRepository()

  println(s"Current resource root is: ${getClass.getResource("/").getPath}")

  def readFile[A](fileName: String)(implicit reads: Reads[A], ct: ClassTag[A]): Seq[A] = {
    Option(getClass.getResource(s"/testdata/$fileName")).map { fileUrl =>
      val js = Json.parse(Source.fromFile(fileUrl.getPath, "UTF-8").mkString)
      Json.fromJson[Seq[A]](js).getOrElse(Seq.empty[A])
    }.getOrElse(throw new RuntimeException(s"Couldn't find the file: $fileName"))
  }

  // Add users
  val users = readFile[User]("users.json")
  users.foreach { usr =>
    println(s"Adding user ${usr.username.value}")
    userRepo.save(usr.copy(password = Crypto.encryptPassword(usr.password)))
  }
}
