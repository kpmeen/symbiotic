package util.testdata

/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */

import core.security.authentication.Crypto
import models.party.{Organisation, User}
import models.project.Project
import play.api.libs.json.{Json, Reads}
import repository.mongodb.party.{MongoDBOrganisationRepository, MongoDBUserRepository}
import repository.mongodb.project.MongoDBProjectRepository

import scala.io.Source
import scala.reflect.ClassTag

object TestDataLoader extends App {

  MongoDBUserRepository.ensureIndex()
  MongoDBOrganisationRepository.ensureIndex()
  MongoDBProjectRepository.ensureIndex()

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
    MongoDBUserRepository.save(usr.copy(password = Crypto.encryptPassword(usr.password)))
  }
  val uids = users.map(_.id.get)

  // Add orgs
  val orgs = readFile[Organisation]("organisations.json")
  orgs.foreach { org =>
    println(s"Adding organisation ${org.name}")
    MongoDBOrganisationRepository.save(org)
  }
  val oids = orgs.map(_.id.get)

  // Add projects
  val projs = readFile[Project]("projects.json")
  projs.foreach { p =>
    println(s"Adding project ${p.title}")
    MongoDBProjectRepository.save(p)
  }
  val pids = projs.map(_.id.get)

}
