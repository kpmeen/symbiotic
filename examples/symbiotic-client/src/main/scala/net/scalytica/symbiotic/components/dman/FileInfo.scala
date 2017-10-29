package net.scalytica.symbiotic.components.dman

import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{ReactComponentB, _}
import net.scalytica.symbiotic.core.converters.DateConverters._
import net.scalytica.symbiotic.core.converters.SizeConverters._
import net.scalytica.symbiotic.css.FileTypes
import net.scalytica.symbiotic.logger._
import net.scalytica.symbiotic.models.dman.ManagedFile
import net.scalytica.symbiotic.models.party.User
import org.scalajs.dom.html.Div

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

object FileInfo {

  object Style extends StyleSheet.Inline {

    import dsl._

    val container = style("fileinfo-container")(
      addClassNames("container-fluid", "center-block", "text-center"),
      paddingTop(20 px),
      paddingBottom(20 px)
    )

    val title = style("fileinfo-title")(
      addClassNames("center-block", "text-center"),
      fontSize(18 px),
      fontWeight bold
    )

    val contentType = style("fileinfo-ctype")(
      addClassNames("center-block", "text-center"),
      fontWeight bold
    )

    val metadata = style("fileinfo-md")(
      addClassNames("row"),
      fontSize(12 px)
    )

    val mdLabel = style("fileinfo-md-label")(
      addClassNames("col-xs-6", "text-right")
    )

    val mdText = style("fileinfo-md-text")(
      addClassNames("col-xs-6", "text-left")
    )
  }

  case class State(
      maybeFile: ExternalVar[Option[ManagedFile]],
      uploadedBy: Option[String] = None,
      lockedBy: Option[String] = None
  )

  class Backend($ : BackendScope[ExternalVar[Option[ManagedFile]], State]) {

    def init(p: ExternalVar[Option[ManagedFile]]): Callback =
      if (p.value.isEmpty) {
        $.modState(
          s => State(maybeFile = p, uploadedBy = None, lockedBy = None)
        )
      } else {
        Callback.future {
          val maybeUplUid = p.value.flatMap(_.metadata.createdBy)
          val maybeLckUid = p.value.flatMap(_.metadata.lock.map(_.by))
          for {
            uplUsr <- fetchUser(maybeUplUid)
            lckUsr <- if (maybeUplUid == maybeLckUid) Future.successful(uplUsr)
                     else fetchUser(maybeLckUid)
          } yield {
            $.modState(
              s => State(maybeFile = p, uploadedBy = uplUsr, lockedBy = lckUsr)
            )
          }
        }
      }

    def fetchUser(userId: Option[String]): Future[Option[String]] =
      userId.map { uid =>
        User.getUser(uid).map {
          case Left(fail) =>
            log.error(
              s"Unable to retrieve user data for $uid because: ${fail.msg}"
            )
            None
          case Right(usr) => Some(usr.readableName)
        }
      }.getOrElse(Future.successful(None))

    // scalastyle:off method.length
    def render(state: State): ReactTagOf[Div] =
      state.maybeFile.value.map { fw =>
        val fileId   = fw.metadata.fid
        val locked   = fw.metadata.lock.isDefined
        val lockIcon = if (locked) "fa fa-lock" else "fa fa-unlock"
        <.div(Style.container)(
          <.p(
            <.i(
              FileTypes.Styles.Icon5x(
                FileTypes.fromContentType(fw.contentType)
              )
            )
          ),
          <.div(Style.title, <.span(fw.filename)),
          <.br(),
          <.div(
            Style.contentType,
            <.span(fw.contentType),
            <.span(" - "),
            <.span(s"${fw.length.map(toReadableSize).getOrElse("N/A")}")
          ),
          <.br(),
          <.div(
            Style.metadata,
            <.label(
              Style.mdLabel,
              ^.`for` := s"fi_version_$fileId",
              "version: "
            ),
            <.span(
              Style.mdText,
              ^.name := s"fi_version_$fileId",
              fw.metadata.version
            )
          ),
          <.div(
            Style.metadata,
            <.label(
              Style.mdLabel,
              ^.`for` := s"fi_uploaded_$fileId",
              "uploaded: "
            ),
            <.span(
              Style.mdText,
              ^.name := s"fi_uploaded_$fileId",
              s"${fw.createdDate.map(toReadableDate).getOrElse("")}"
            )
          ),
          <.div(
            Style.metadata,
            <.label(Style.mdLabel, ^.`for` := s"fi_by_$fileId", "by: "),
            <.span(
              Style.mdText,
              ^.name := s"fi_by_$fileId",
              s"${state.uploadedBy.getOrElse("")}"
            )
          ),
          <.br(),
          <.div(Style.contentType, <.span(<.i(^.className := lockIcon))),
          fw.metadata.lock.map { l =>
            <.div(
              <.div(
                Style.metadata,
                <.label(
                  Style.mdLabel,
                  ^.`for` := s"fi_lockby_$fileId",
                  "locked by: "
                ),
                <.span(
                  Style.mdText,
                  ^.name := s"fi_lockby_$fileId",
                  s"${state.lockedBy.getOrElse("")}"
                )
              ),
              <.div(
                Style.metadata,
                <.label(
                  Style.mdLabel,
                  ^.`for` := s"fi_lockdate_$fileId",
                  "since: "
                ),
                <.span(
                  Style.mdText,
                  ^.name := s"fi_lockdate_$fileId",
                  s"${toReadableDate(l.date)}"
                )
              )
            )
          }.getOrElse(EmptyTag)
        )
      }.getOrElse(
        <.div(
          Style.container,
          <.span(
            ^.className := "text-muted",
            "Select a file to see its metadata"
          )
        )
      )

    // scalastyle:on method.length
  }

  val component = ReactComponentB[ExternalVar[Option[ManagedFile]]]("FileInfo")
    .initialState_P(p => State(p))
    .renderBackend[Backend]
    .componentWillMount($ => $.backend.init($.props))
    .shouldComponentUpdate(
      ctx => ctx.nextProps.value != ctx.currentState.maybeFile.value
    )
    .componentWillUpdate(ctx => ctx.$.backend.init(ctx.$.props))
    .build

  def apply(fw: ExternalVar[Option[ManagedFile]]) = component(fw)
}
