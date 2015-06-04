enablePlugins(ScalaJSPlugin)

name := "symbiotic-web"
version := "1.0"
scalaVersion := "2.11.6"


// Create launcher file that searches for an object that extends JSApp.
// Make sure there is only one!
persistLauncher := true
persistLauncher in Test := false

// Configuring the workbench that launches a node server.
workbenchSettings
bootSnippet := "net.scalytica.symbiotic.SymbioticApp().main();"


// Dependency management...
val scalaJSReactVersion = "0.9.0"
val scalaCssVersion = "0.2.0"
val scalazVersion = "7.1.2"

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion)

// For some reason, the following dependencies need to be disambiguated.
dependencyOverrides += "org.scala-lang" % "scala-library" % "2.11.6"
dependencyOverrides += "org.scala-js" %% "scalajs-library" % scalaJSVersion
dependencyOverrides += "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion
dependencyOverrides += "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion
dependencyOverrides += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % scalazVersion

// React itself
//   (react-with-addons.js can be react.js, react.min.js, react-with-addons.min.js)
// DOM, which doesn't exist by default in the Rhino runner. To make the DOM available in Rhino
jsDependencies ++= Seq(
  "org.webjars" % "react" % "0.12.1" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React"
)

// creates single js resource file for easy integration in html page
skip in packageJSDependencies := false


// copy  javascript files to js folder,that are generated using fastOptJS/fullOptJS

crossTarget in(Compile, fullOptJS) := file("js")

crossTarget in(Compile, fastOptJS) := file("js")

crossTarget in(Compile, packageJSDependencies) := file("js")

crossTarget in(Compile, packageScalaJSLauncher) := file("js")

crossTarget in(Compile, packageMinifiedJSDependencies) := file("js")

artifactPath in(Compile, fastOptJS) := ((crossTarget in(Compile, fastOptJS)).value /
  ((moduleName in fastOptJS).value + "-opt.js"))



scalacOptions += "-feature"

