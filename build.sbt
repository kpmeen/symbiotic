name := """symbiotic-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  """-deprecation""", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  //  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
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

// Scalariform source code formatting
//defaultScalariformSettings // formatting must be triggered manually using scalariformFormat
//scalariformSettings // formatting is triggered automatically at the compile step
//ScalariformKeys.preferences := ScalariformKeys.preferences.value
//    .setPreference(FormatXml, false)
//    .setPreference(SpacesAroundMultiImports, false)
//    .setPreference(PreserveDanglingCloseParenthesis, true)

// Dependency resolvers
resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
resolvers += "Scalaz Bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

// Dependency managmeent
libraryDependencies ++= Seq(
  cache,
  ws
)

// Versions
val specs2Version = "3.6.2"
val akkaVersion = "2.3.11"
val slf4jVersion = "1.7.12"

val silhouetteVersion = "3.0.0"

// MongoDB
libraryDependencies += "org.mongodb" %% "casbah" % "2.8.2"

// Akka and akka Persistence...for event sourcing
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion
)

// Crypto
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion
)

// Silhouette
libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-testkit" % silhouetteVersion % "test"
)

// Testing
libraryDependencies ++= Seq(
  "org.specs2" %% "specs2-core" % specs2Version % "test",
  "org.specs2" %% "specs2-html" % specs2Version % "test",
  "org.specs2" %% "specs2-junit" % specs2Version % "test",
  "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test"
)

dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % akkaVersion
dependencyOverrides += "org.slf4j" % "slf4j-api" % slf4jVersion