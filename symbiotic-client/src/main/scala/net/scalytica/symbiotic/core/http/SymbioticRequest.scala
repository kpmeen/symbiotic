package net.scalytica.symbiotic.core.http

import net.scalytica.symbiotic.core.session.Session
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData

object SymbioticRequest {

  val XAuthTokenHeader = "X-Auth-Token"

  private def authHeaders(orig: Map[String, String]): Map[String, String] =
    Session.token
      .map(t => orig ++ Map("X-Auth-Token" -> t.token))
      .getOrElse(orig)

  def get(
      url: String,
      data: InputData = null,
      timeout: Int = 0,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false,
      responseType: String = ""
  ) =
    Ajax.get(
      url,
      data,
      timeout,
      authHeaders(headers),
      withCredentials,
      responseType
    )

  def post(
      url: String,
      data: InputData = null,
      timeout: Int = 0,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false,
      responseType: String = ""
  ) =
    Ajax.post(
      url,
      data,
      timeout,
      authHeaders(headers),
      withCredentials,
      responseType
    )

  def put(
      url: String,
      data: InputData = null,
      timeout: Int = 0,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false,
      responseType: String = ""
  ) =
    Ajax.put(
      url,
      data,
      timeout,
      authHeaders(headers),
      withCredentials,
      responseType
    )

  def delete(
      url: String,
      data: InputData = null,
      timeout: Int = 0,
      headers: Map[String, String] = Map.empty,
      withCredentials: Boolean = false,
      responseType: String = ""
  ) =
    Ajax.delete(
      url,
      data,
      timeout,
      authHeaders(headers),
      withCredentials,
      responseType
    )

}
