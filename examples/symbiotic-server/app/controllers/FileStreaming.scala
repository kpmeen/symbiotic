package controllers

import java.net.URLEncoder.encode

import net.scalytica.symbiotic.api.types.SymbioticDocument
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait FileStreaming { self: SymbioticController =>

  val CT_DISP_ATTACHMENT = "attachment"
  val CT_DISP_INLINE     = "inline"

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param maybeFile Option[GridFSDocument]
   * @return Result (Ok or NotFound)
   */
  def serve(maybeFile: Option[SymbioticDocument[_]]): Result =
    maybeFile.map(fw => serve(fw)).getOrElse(NotFound)

  /**
   * Serves a file by streaming the contents back as chunks to the client.
   *
   * @param file GridFSDocument[_]
   * @return Result (Ok)
   */
  def serve(
      file: SymbioticDocument[_],
      dispMode: String = CT_DISP_ATTACHMENT
  ): Result =
    file.stream.map { source =>
      val cd =
        s"""$dispMode; filename="${file.filename}"; filename*=UTF-8''""" +
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
      ff: Future[SymbioticDocument[_]]
  )(implicit ec: ExecutionContext): Future[Result] =
    ff.map(fw => serve(fw)).recover {
      case NonFatal(_) => NotFound
    }

}
