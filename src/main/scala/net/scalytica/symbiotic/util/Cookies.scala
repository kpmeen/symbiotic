/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.util

import org.scalajs.dom

import scala.scalajs.js.{URIUtils, Date}

object Cookies {

  def set(cookieName: String, args: Map[String, String]): Unit = {
    val expiresAt = new Date(year = 9999, month = 12).toUTCString()
    val argStr = args.toList.map(kv => s"${kv._1}=${kv._2}").mkString("&")
    val cookieValue = s"$argStr; expires=$expiresAt; path=/"
    dom.document.cookie = s"$cookieName=${URIUtils.encodeURI(cookieValue)}"
  }

  def remove(cookieName: String): Unit = {
    val expiresAt = new Date(Date.now()).toUTCString()
    dom.document.cookie = s"$cookieName= ; expires=$expiresAt; path=/"
  }

  def get(cookieName: String): Option[String] =
    Option(dom.document.cookie).flatMap(_.split(';').find(_.startsWith(cookieName)))

}
