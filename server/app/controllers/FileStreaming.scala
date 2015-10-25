/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.net.URLEncoder.encode

import models.docmanagement.File
import play.api.mvc.{Controller, Result}

import scala.concurrent.{ExecutionContext, Future}

trait FileStreaming {
  self: Controller =>

  val CT_DISP_ATTACHMENT = "attachment"
  val CT_DISP_INLINE = "inline"

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file File
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok)
   */
  def serve(file: File, dispositionMode: String = CT_DISP_ATTACHMENT)(implicit ec: ExecutionContext): Result =
    file.enumerate.map { fenum =>
      Ok.chunked(fenum).withHeaders(
        CONTENT_DISPOSITION -> (s"""$dispositionMode; filename="${file.filename}"; filename*=UTF-8''""" + encode(file.filename, "UTF-8").replace("+", "%20"))
      )
    }.getOrElse(NotFound)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param maybeFile Option[File]
   * @param ec ExecutionContext required due to using Futures
   * @return Result (Ok or NotFound)
   */
  def serve(maybeFile: Option[File])(implicit ec: ExecutionContext): Result =
    maybeFile.map(fw => serve(fw)).getOrElse(NotFound)

  /**
   * Serves a Future file by streaming the content back as chunks to the client.
   *
   * @param futureFile Future[File]
   * @param ec ExecutionContext required due to using Futures
   * @return Future[Result] (Ok or NotFound)
   */
  def serve(futureFile: Future[File])(implicit ec: ExecutionContext): Future[Result] =
    futureFile.map(fw => serve(fw)).recover {
      case _ => NotFound
    }

}