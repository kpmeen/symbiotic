enablePlugins(ScalaJSPlugin)

name := "Symbiotic Web"

scalaVersion := "2.11.5" // or any other Scala version >= 2.10.2

libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.8.0"

// Minimal usage
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "core" % "0.8.4"

// React itself
//   (react-with-addons.js can be react.js, react.min.js, react-with-addons.min.js)
jsDependencies += "org.webjars" % "react" % "0.12.1" / "react-with-addons.js" commonJSName "React"

// Test support including ReactTestUtils
//   (requires react-with-addons.js instead of just react.js)
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "test" % "0.8.4" % "test"

// Scalaz support
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % "0.8.4"

// Monocle support
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "ext-monocle" % "0.8.4"

// Extra features (includes Scalaz and Monocle support)
libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % "0.8.4"