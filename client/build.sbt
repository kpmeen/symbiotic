name := "symbiotic-web"
version := "1.0"
scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  "-feature",
  """-deprecation""",
  //  "-Xlint",
  //  "-Xfatal-warnings",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

lazy val root = (project in file(".")).enablePlugins(ScalaJSPlugin)

// Create launcher file that searches for an object that extends JSApp.
// Make sure there is only one!
persistLauncher := true
persistLauncher in Test := false

scalaJSStage in Global := FastOptStage

// See more at: http://typesafe.com/blog/improved-dependency-management-with-sbt-0137#sthash.7hS6gFEu.dpuf
updateOptions := updateOptions.value.withCachedResolution(true)

// Dependency management...
val scalaJSReactVersion = "0.11.0"
val scalaCssVersion = "0.4.1"
val scalazVersion = "7.1.2"
val monocleVersion = "1.2.0-2"
val uPickleVersion = "0.3.9-KP"

libraryDependencies ++= Seq(
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-scalaz72" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-monocle" % scalaJSReactVersion,
  "com.github.japgolly.fork.monocle" %%%! s"monocle-core" % monocleVersion,
  "com.github.japgolly.fork.monocle" %%%! s"monocle-macro" % monocleVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "com.lihaoyi" %%% "upickle" % uPickleVersion
)

val reactJsVersion = "15.0.1"

jsDependencies ++= Seq(
  "org.webjars.bower" % "react" % reactJsVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "react" % reactJsVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
  "org.webjars.bower" % "react" % reactJsVersion / "react-dom-server.js" minified "react-dom-server.min.js" dependsOn "react-dom.js" commonJSName "ReactDOMServer",
  "org.webjars" % "log4javascript" % "1.4.10" / "js/log4javascript.js"
)

// creates single js resource file for easy integration in html page
skip in packageJSDependencies := false


// copy javascript files to js folder,that are generated using fastOptJS/fullOptJS

crossTarget in(Compile, fullOptJS) := file("js")
crossTarget in(Compile, fastOptJS) := file("js")
crossTarget in(Compile, packageJSDependencies) := file("js")
crossTarget in(Compile, packageScalaJSLauncher) := file("js")
crossTarget in(Compile, packageMinifiedJSDependencies) := file("js")

artifactPath in(Compile, fastOptJS) := ((crossTarget in(Compile, fastOptJS)).value / ((moduleName in fastOptJS).value + "-opt.js"))
