name := """symbiotic-server"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

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

// Versions
val specs2Version = "3.6.2"
val akkaVersion = "2.3.11"
val slf4jVersion = "1.7.12"

// MongoDB
libraryDependencies += "org.mongodb" %% "casbah" % "2.8.1"

// Akka and akka Persistence...for event sourcing
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-experimental" % akkaVersion,
  "com.github.scullxbones" %% "akka-persistence-mongo-casbah" % "0.3.0"
)

// Crypto
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.3m"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion
)

// Testing
libraryDependencies ++= Seq(
//  "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "1.47.2" % "test",
  "org.specs2" %% "specs2-core" % specs2Version % "test",
  "org.specs2" %% "specs2-html" % specs2Version % "test",
  "org.specs2" %% "specs2-junit" % specs2Version % "test",
  "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test"
)

dependencyOverrides += "com.typesafe" % "config" % "1.3.0"
dependencyOverrides += "com.typesafe.akka" %% "akka-actor" % akkaVersion
dependencyOverrides += "org.apache.httpcomponents" % "httpclient" % "4.3.4"
dependencyOverrides += "org.apache.httpcomponents" % "httpcore" % "4.3.2"
dependencyOverrides += "commons-codec" % "commons-codec" % "1.10"
dependencyOverrides += "commons-logging" % "commons-logging" % "1.1.3"
dependencyOverrides += "org.slf4j" % "slf4j-api" % slf4jVersion
dependencyOverrides += "joda-time" % "joda-time" % "2.7"
dependencyOverrides += "org.joda" % "joda-convert" % "1.7"
dependencyOverrides += "com.google.guava" % "guava" % "18.0"
dependencyOverrides += "org.pegdown" % "pegdown" % "1.4.0"
dependencyOverrides += "junit" % "junit" % "4.12"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.5.4"
dependencyOverrides += "org.fusesource.leveldbjni" % "leveldbjni" % "1.7"
dependencyOverrides += "org.scala-lang" % "scala-library" % "2.11.7"
dependencyOverrides += "org.scala-lang" % "scala-compiler" % "2.11.7"
dependencyOverrides += "org.scala-lang" % "scala-reflect" % "2.11.7"
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
dependencyOverrides += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"