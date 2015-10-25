/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.core.session

import org.scalajs.dom.localStorage

object Session {

  val UserCookie = "SYMBIOTIC_USER"

  val usernameKey = "username"
  val userIdKey = "uid"

  val storage = localStorage

  def clear(): Unit = Cookies.remove(UserCookie)

  def init(uname: String, uid: String): Unit = Cookies.set(UserCookie, Map(usernameKey -> uname, userIdKey -> uid))

  def validate: Boolean = {
    val entries = Cookies.toMap(UserCookie)
    entries.contains(usernameKey) && entries.contains(userIdKey)
  }

  def userId: Option[String] = Cookies.valueOf(UserCookie, userIdKey)

  def username: Option[String] = Cookies.valueOf(UserCookie, usernameKey)

}
