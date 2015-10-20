name := "symbiotic-web"
version := "1.0"
scalaVersion := "2.11.7"

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
val scalaJSReactVersion = "0.10.0"
val scalaCssVersion = "0.3.1"
val scalazVersion = "7.1.2"
val monocleVersion = "1.1.1"

libraryDependencies ++= Seq(
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
  "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-monocle" % scalaJSReactVersion,
  "com.github.japgolly.fork.monocle" %%%! s"monocle-core" % monocleVersion,
  "com.github.japgolly.fork.monocle" %%%! s"monocle-macro" % monocleVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "com.lihaoyi" %%% "upickle" % "0.3.6-KP"
)

// For some reason, the following dependencies need to be disambiguated.
dependencyOverrides += "org.scala-lang" % "scala-library" % "2.11.7"
dependencyOverrides += "org.scala-lang" % "scala-reflect" % "2.11.7"
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
dependencyOverrides += "org.scala-js" %% "scalajs-library" % scalaJSVersion
dependencyOverrides += "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion
dependencyOverrides += "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion
dependencyOverrides += "com.github.japgolly.fork.scalaz" %%% "scalaz-core" % scalazVersion

jsDependencies ++= Seq(
  "org.webjars.npm" % "react"     % "0.14.0" / "react-with-addons.js" commonJSName "React"    minified "react-with-addons.min.js",
  "org.webjars.npm" % "react-dom" % "0.14.0" / "react-dom.js"         commonJSName "ReactDOM" minified "react-dom.min.js" dependsOn "react-with-addons.js",
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
