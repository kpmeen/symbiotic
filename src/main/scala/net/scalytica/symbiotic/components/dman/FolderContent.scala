/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.extra.{LogLifecycle, Reusability}
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.logger.log
import net.scalytica.symbiotic.models.dman._
import net.scalytica.symbiotic.routes.DMan.FolderPath

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.util.{Failure, Success}
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FolderContent {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style(addClassNames("container"))
    val card = style(addClassNames("card", "medium"))
    val cardContent = style(addClassNames("card-content"))
    val cardTitle = style(addClassNames("card-title", "grey-text", "text-darken-4"))

    val folder = style(addClassNames("fa", "fa-folder-o"))
    val file = style(addClassNames("fa", "fa-file-o"))
  }

  case class Props(cid: String, folder: Option[String], fw: Seq[FileWrapper], ctl: RouterCtl[FolderPath])

  class Backend(t: BackendScope[Props, Props]) {
    def loadContent(): Unit = {
      loadContent(t.props)
      t.forceUpdate()
    }

    def loadContent(p: Props): Unit = {
      if (p.fw.nonEmpty) {
        log.info(p)
        t.modState(_.copy(folder = p.folder, fw = p.fw))
      } else {
        FileWrapper.loadF(p.cid, p.folder).onComplete {
          case Success(res) =>
            log.info(p)
            t.modState(_.copy(folder = p.folder, fw = res))
          case Failure(err) =>
            log.error(err)
            t.modState(_.copy(folder = p.folder, fw = Nil))
        }
      }
    }
  }

  implicit val fwReuse = Reusability.fn((p: Props, s: Props) => p.folder == s.folder)

  val component = ReactComponentB[Props]("FolderContent")
    .initialStateP(p => p)
    .backend(new Backend(_))
    .render((p, s, b) =>
    <.div(Style.container,
      <.p(s"Content for folder ${s.folder.getOrElse("/")} is:"),
      if (s.fw.nonEmpty) {
        <.div(
          s.fw.map(w =>
            <.div(
              if (w.isFolder.get) <.i(Style.folder, s" ${w.simpleFolderName}")
              else <.i(Style.file, s" ${w.filename}")
            )
          )
        )
      } else {
        <.span("Folder is empty")
      }
    ))
    .configure(LogLifecycle.short)
    .configure(Reusability.shouldComponentUpdate)
    .componentWillMount(_.backend.loadContent())
    .componentWillReceiveProps((csm, p) => csm.backend.loadContent(p))
    .build

  def apply(p: Props) = component(p)

  def apply(cid: String, folder: Option[String], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, folder, Nil, ctl))

  def apply(cid: String, folder: Option[String], fw: Seq[FileWrapper], ctl: RouterCtl[FolderPath]) =
    component(Props(cid, folder, fw, ctl))
}