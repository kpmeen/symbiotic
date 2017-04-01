/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package controllers

import java.net.URLEncoder.encode

import net.scalytica.symbiotic.data.GridFSDocument
import play.api.mvc.{Controller, Result}

import scala.concurrent.{ExecutionContext, Future}

trait FileStreaming { self: Controller =>

  val CT_DISP_ATTACHMENT = "attachment"
  val CT_DISP_INLINE     = "inline"

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param maybeFile Option[GridFSDocument]
   * @param ec        ExecutionContext required due to using Futures
   * @return Result (Ok or NotFound)
   */
  def serve(
      maybeFile: Option[GridFSDocument[_]]
  )(implicit ec: ExecutionContext): Result =
    maybeFile.map(fw => serve(fw)).getOrElse(NotFound)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file GridFSDocument[_]
   * @param ec   ExecutionContext required due to using Futures
   * @return Result (Ok)
   */
  def serve(
      file: GridFSDocument[_],
      dispMode: String = CT_DISP_ATTACHMENT
  )(implicit ec: ExecutionContext): Result =
    file.source.map { source =>
      val cd = s"""$dispMode; filename="${file.filename}"; filename*=UTF-8''""" +
        encode(file.filename, "UTF-8").replace("+", "%20")
      Ok.chunked(source).withHeaders(CONTENT_DISPOSITION -> cd)
    }.getOrElse(NotFound)

  /**
   * Serves a Future file by streaming the content back as chunks to the client.
   *
   * @param ff Future[GridFSDocument]
   * @param ec ExecutionContext required due to using Futures
   * @return Future[Result] (Ok or NotFound)
   */
  def serve(
      ff: Future[GridFSDocument[_]]
  )(implicit ec: ExecutionContext): Future[Result] =
    ff.map(fw => serve(fw)).recover {
      case _ => NotFound
    }

}
