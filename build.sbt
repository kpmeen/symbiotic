name := """symbiotic-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

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

// In case the project is being compiled with Java 8 we need to enforce Java 7 compatibility.
javacOptions ++= Seq(
  "-Xlint:deprecation"
)

// Test options
scalacOptions in Test ++= Seq("-Yrangepos")
testOptions += Tests.Argument(TestFrameworks.Specs2, "html", "junitxml", "console")

// Play router configuration
routesGenerator := InjectedRoutesGenerator

// Dependency resolvers
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Scalaz Bintray" at "http://dl.bintray.com/scalaz/releases"

// Dependency managmeent
libraryDependencies ++= Seq(
  cache,
  ws
)

// MongoDB
libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

// Crypto
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.12"
)

// Testing
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.4" % "test",
  "org.specs2" %% "specs2-html" % "3.4" % "test",
  "org.specs2" %% "specs2-junit" % "3.4" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.4" % "test",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.47.2" % "test"
)

dependencyOverrides += "org.apache.httpcomponents" % "httpclient" % "4.3.4"
dependencyOverrides += "com.google.guava" % "guava" % "18.0"
dependencyOverrides += "org.pegdown" % "pegdown" % "1.4.0"
dependencyOverrides += "commons-logging" % "commons-logging" % "1.1.3"
dependencyOverrides += "junit" % "junit" % "4.12"
dependencyOverrides += "org.apache.httpcomponents" % "httpcore" % "4.3.2"
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.0.3"