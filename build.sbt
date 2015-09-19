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
val scalaJSReactVersion = "0.9.2"
val scalaCssVersion = "0.3.0"
val scalazVersion = "7.1.2"

libraryDependencies ++= Seq(
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full),
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-scalaz71" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "ext-monocle" % scalaJSReactVersion,
  "com.github.japgolly.fork.monocle" %%%! s"monocle-core" % "1.1.1",
  "com.github.japgolly.fork.monocle" %%%! s"monocle-macro" % "1.1.1",
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "com.lihaoyi" %%% "upickle" % "0.2.8-KP"
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

// React itself
//   (react-with-addons.js can be react.js, react.min.js, react-with-addons.min.js)
// DOM, which doesn't exist by default in the Rhino runner. To make the DOM available in Rhino
jsDependencies ++= Seq(
  "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
  "org.webjars.bower" % "jquery" % "2.1.3" / "dist/jquery.js" commonJSName "jQuery",
  "org.webjars.bower" % "materialize" % "0.96.1" / "js/materialize.js" dependsOn "dist/jquery.js",
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
