package scalajsreact.template.components.items

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object Item2Data {
  def onTextChange(e: ReactEventI): Unit = {
    println("Value received = " + e.target.value)
  }

  def onButtonPressed(): Unit =  {
    println("The button was pressed!")
  }

   val component = ReactComponentB.static("Item2",
     <.div("Open console and type or click", <.br,
       <.input(
         ^.`type`    := "text",
         ^.onChange ==> onTextChange),
       <.button(
         ^.onClick --> onButtonPressed,
         "Press me!"))
   ).buildU

   def apply() = component()
 }


