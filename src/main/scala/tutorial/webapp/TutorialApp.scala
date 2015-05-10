package tutorial.webapp

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.dom
import dom.document
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

object TutorialApp extends JSApp {
  def main(): Unit = {
  	val HelloMessage = ReactComponentB[String]("HelloMessage")
	   .render(name => <.div("Hello ", name))
	   .build

	React.render(HelloMessage("John"), document.body)
  }
}
