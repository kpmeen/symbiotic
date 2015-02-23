name := """copr8"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

scalacOptions ++= Seq(
  "-feature",
  """-deprecation""",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"


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
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.46.4" % "test"
)