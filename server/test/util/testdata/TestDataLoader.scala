package util.testdata

/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import models.party.CreateUser
import models.party.PartyBaseTypes.UserId
import play.api.libs.json.{Json, Reads}
import repository.mongodb.party.MongoDBUserRepository
import repository.mongodb.silhouette.MongoDBPasswordAuthRepository

import scala.io.Source
import scala.reflect.ClassTag

object TestDataLoader extends App {

  val userRepo = new MongoDBUserRepository()
  val credsRepo = new MongoDBPasswordAuthRepository()
  val passwdHasher = new BCryptPasswordHasher()

  println(s"Current resource root is: ${getClass.getResource("/").getPath}")

  def readFile[A](fileName: String)(implicit reads: Reads[A], ct: ClassTag[A]): Seq[A] = {
    Option(getClass.getResource(s"/testdata/$fileName")).map { fileUrl =>
      val js = Json.parse(Source.fromFile(fileUrl.getPath, "UTF-8").mkString)
      Json.fromJson[Seq[A]](js).getOrElse(Seq.empty[A])
    }.getOrElse(throw new RuntimeException(s"Couldn't find the file: $fileName"))
  }

  // Add users
  val users = readFile[CreateUser]("users.json")
  users.foreach { usr =>
    val loginInfo = LoginInfo(CredentialsProvider.ID, usr.username.value)
    val theUser = usr.copy(password2 = usr.password1).toUser(UserId.createOpt(), loginInfo)
    println(s"Adding user ${theUser.username.value}")
    userRepo.save(theUser)
    credsRepo.save(loginInfo, passwdHasher.hash(usr.password1.value))
  }
}
