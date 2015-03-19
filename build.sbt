name := """symbiotic"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

scalacOptions ++= Seq(
  "-feature",
  """-deprecation""",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-target:jvm-1.7"
)

// In case the project is being compiled with Java 8 we need to enforce Java 7 compatibility.
javacOptions ++= Seq(
  "-Xlint:deprecation",
  "-source", "1.7",
  "-target", "1.7"
)

// Test options
scalacOptions in Test ++= Seq("-Yrangepos")
testOptions += Tests.Argument(TestFrameworks.Specs2, "html", "junitxml", "console")

// Dependency resolvers
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Scalaz Bintray" at "http://dl.bintray.com/scalaz/releases"

// Dependency managmeent
libraryDependencies ++= Seq(
  cache,
  ws
)

// MongoDB
libraryDependencies += "org.mongodb" %% "casbah" % "2.8.0"

// Crypto
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

// Testing
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % "3.1" % "test",
  "org.specs2" %% "specs2-html" % "3.1" % "test",
  "org.specs2" %% "specs2-junit" % "3.1" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "3.1" % "test",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.47.0" % "test"
)
