//package net.scalytica.symbiotic.components
//
//import japgolly.scalajs.react._
//import japgolly.scalajs.react.vdom.prefix_<^._
//
//import scalacss.Defaults._
//import scalacss.ScalaCssReact._
//
//object Footer {
//
//  object Style extends StyleSheet.Inline {
//
//    import dsl._
//
//    val footer = style(
//      addClassNames("page-footer", "transparent"),
//      paddingTop.`0`.important,
//      marginTop(10.px).important
//    )
//
//    val copyright = style(
//      addClassNames("footer-copyright"),
//      backgroundColor.indigo
//    )
//
//  }
//
//  val component = ReactComponentB.static("Footer",
//    <.footer(Style.footer,
//      <.div(Style.copyright,
//        <.div(Material.container,
//          "© ... all rights reserved."
//        )
//      )
//    )
//  ).buildU
//
//  def apply() = component()
//}
