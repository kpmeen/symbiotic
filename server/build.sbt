import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import sbt._

import scalariform.formatter.preferences._

name := """symbiotic-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      name, version, scalaVersion, sbtVersion, buildInfoBuildNumber
    ),
    buildInfoPackage := "net.scalytica.symbiotic.server"
  )

buildInfoOptions += BuildInfoOption.ToJson

scalaVersion := "2.11.8"

scalacOptions ++= Seq(
  """-deprecation""", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
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

PlayKeys.playOmnidoc := false

// Play router configuration
routesGenerator := InjectedRoutesGenerator

// Exclude generated stuff from coverage reports
coverageExcludedPackages :=
  "<empty>;router;controllers.Reverse*Controller;controllers.javascript.*;" +
    "models.base.*;models.party.*;models.project.*;" +
    "models.docmanagement.MetadataKeys.*;" +
    "models.docmanagement.Implicits.*;"

// Scalariform source code formatting
// formatting is triggered automatically at the compile step
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(SpacesAroundMultiImports, false)

// Docker packaging configuration
maintainer in Docker := "Knut Petter Meen <kp@scalytica.net>"
packageSummary in Docker := "Symbiotic Backend services"
packageDescription in Docker := "Backend for the Symbiotic simple file management system"
dockerExposedPorts in Docker := Seq(9000)
dockerBaseImage in Docker := "java:8"
dockerRepository := Some("registry.gitlab.com/kpmeen")
dockerAlias := DockerAlias(Some("registry.gitlab.com"), Some("kpmeen"), "symbiotic", Some("latest"))

// Dependency resolvers
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += Resolver.bintrayRepo("scalaz", "releases")
resolvers += Resolver.jcenterRepo
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"

updateOptions := updateOptions.value.withCachedResolution(true)

// Dependency managmeent
libraryDependencies ++= Seq(
  cache,
  ws,
  filters
)

// Versions
val specs2Version = "3.8.5"
val akkaVersion = "2.4.16"
val logbackVersion = "1.1.8"
val slf4jVersion = "1.7.22"
val playSlickVersion = "2.0.2"
val casbahVersion = "3.1.1"
val silhouetteVersion = "4.0.0"

// DB stuff
libraryDependencies += "org.mongodb" %% "casbah" % casbahVersion
//libraryDependencies ++= Seq(
//  "com.typesafe.play" %% "play-slick" % playSlickVersion,
//  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion
//)

// Akka and akka Persistence...for event sourcing
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

// Crypto
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion
)

// Scala Guice DSL
libraryDependencies += "net.codingwell" %% "scala-guice" % "4.1.0"
// Ficus config readers
libraryDependencies += "com.iheart" %% "ficus" % "1.4.0"

// Silhouette
libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-password-bcrypt" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-crypto-jca" % silhouetteVersion,
  "com.mohiva" %% "play-silhouette-persistence" % silhouetteVersion,
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
dependencyOverrides += "com.typesafe.akka" %% "akka-stream" % akkaVersion
dependencyOverrides += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
dependencyOverrides += "org.slf4j" % "slf4j-api" % slf4jVersion
dependencyOverrides += "ch.qos.logback" % "logback-core" % logbackVersion
dependencyOverrides += "ch.qos.logback" % "logback-classic" % logbackVersion
