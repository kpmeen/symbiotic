import Dependencies._
import Settings._
import play.sbt.PlayImport
import play.sbt.routes.RoutesKeys
import sbt.Keys._
import sbt._

import scala.sys.process._
import scala.language.postfixOps

// scalastyle:off
/*

    Build script for Symbiotic, a light weight document management library.

 */

name := """symbiotic"""

// ============================================================================
// Custom task definitions
// ============================================================================
lazy val startBackends =
  taskKey[Unit]("Bootstrap Docker containers with databases and ElasticSearch.")
lazy val stopBackends =
  taskKey[Unit]("Stop Docker containers with DB backends.")
lazy val cleanBackends =
  taskKey[Unit]("Remove containers and folders related to DB backends.")
lazy val resetBackends =
  taskKey[Unit]("Stops, cleans and starts backend containers.")
lazy val statusBackends =
  taskKey[Unit]("Prints the running status of the containers")

startBackends := { "./backends.sh start" ! }
stopBackends := { "./backends.sh stop" ! }
cleanBackends := { "./backends.sh clean" ! }
resetBackends := { "./backends.sh reset" ! }
statusBackends := { "./backends.sh status" ! }

// ============================================================================
// Project definitions
// ============================================================================
lazy val symbiotic = (project in file("."))
  .settings(NoPublish)
  .settings(BaseSettings: _*)
  .aggregate(
    sharedLib,
    fsLib,
    coreLib,
    json,
    mongodb,
    postgres,
    elasticSearch,
    playExtras,
    testKit,
    server
  )

lazy val sharedLib = SymbioticProject("shared")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      JodaTime,
      JodaConvert
    ) ++ Akka ++ Logback
  )
  .settings(
    libraryDependencies ++= Seq(
      ScalaTest.scalaTest % Test,
      ScalaTest.scalactic % Test
    ) ++ Logback.map(_    % Test)
  )
  .settings(
    coverageExcludedPackages :=
      "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
        "net.scalytica.symbiotic.data.Implicits.*;"
  )
  .settings(BintrayPublish: _*)

lazy val fsLib = SymbioticProject("fs")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(IHeartFicus) ++ Akka
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .settings(BintrayPublish: _*)

lazy val coreLib = SymbioticProject("core")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(libraryDependencies += IHeartFicus)
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test, mongodb % Test, postgres % Test)
  .settings(BintrayPublish: _*)

lazy val json = SymbioticProject("json")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayJson,
      PlayJsonJoda
    )
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .settings(BintrayPublish: _*)

lazy val mongodb = SymbioticProject("mongodb")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(libraryDependencies ++= Seq(IHeartFicus) ++ Akka ++ MongoDbDriver)
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test)
  .settings(BintrayPublish: _*)

lazy val postgres = SymbioticProject("postgres")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      PlayJson,
      PlayJsonJoda,
      Postgres
    ) ++ Akka ++ Slick ++ SlickPg
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib, fsLib, json)
  .dependsOn(testKit % Test)
  .settings(BintrayPublish: _*)

lazy val elasticSearch = SymbioticProject("elasticsearch")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(IHeartFicus) ++ Elastic4s ++ ApacheLog4j
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib, json)
  .dependsOn(testKit % Test, mongodb % Test, postgres % Test)
  .settings(BintrayPublish: _*)

lazy val playExtras = SymbioticProject("play")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(libraryDependencies ++= Seq(PlayImport.ws, ScalaGuice))
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(coreLib)
  .dependsOn(testKit % Test)
  .settings(BintrayPublish: _*)

lazy val testKit = SymbioticProject("testkit")
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      ScalaTest.scalaTest,
      ScalaTest.scalactic,
      Postgres
    ) ++ AkkaTestKits(Compile) ++ MongoDbDriver
  )
  .dependsOn(sharedLib)
  .settings(BintrayPublish: _*)

lazy val client =
  SymbioticProject("client", Some("examples"))
    .enablePlugins(ScalaJSPlugin, SbtNativePackager, DockerPlugin)
    .settings(fork in Test := false)
    .settings(NoPublish)
    .settings(scalaJSUseMainModuleInitializer := true)
    .settings(scalaJSUseMainModuleInitializer in Test := false)
    .settings(scalaJSStage in Global := FastOptStage)
    .settings(skip in packageJSDependencies := false)
    .settings(ClientDependencies.settings)
    .settings(
      Seq(
        crossTarget in (Compile, fullOptJS) := file(
          s"examples/${name.value}/js"
        ),
        crossTarget in (Compile, fastOptJS) := file(
          s"examples/${name.value}/js"
        ),
        crossTarget in (Compile, packageJSDependencies) := file(
          s"examples/${name.value}/js"
        ),
        crossTarget in (Compile, scalaJSUseMainModuleInitializer) := file(
          s"examples/${name.value}/js"
        ),
        crossTarget in (Compile, packageMinifiedJSDependencies) := file(
          s"examples/${name.value}/js"
        ),
        artifactPath in (Compile, fastOptJS) := ((crossTarget in (Compile, fastOptJS)).value / ((moduleName in fastOptJS).value + "-opt.js"))
      )
    )

lazy val server = SymbioticProject("server", Some("examples"))
  .enablePlugins(PlayScala, BuildInfoPlugin, SbtNativePackager, DockerPlugin)
  .settings(scalacOptions ++= ExperimentalScalacOpts)
  .settings(
    buildInfoKeys := BuildInfoKey.ofN(
      name,
      version,
      scalaVersion,
      sbtVersion,
      buildInfoBuildNumber
    ),
    buildInfoPackage := "net.scalytica.symbiotic.server",
    buildInfoOptions += BuildInfoOption.ToJson
  )
  .settings(PlayKeys.playOmnidoc := false)
  .settings(
    routesGenerator := InjectedRoutesGenerator,
    RoutesKeys.routesImport := Seq.empty
  )
  .settings(
    coverageExcludedPackages :=
      "<empty>;router;controllers.Reverse*Controller;" +
        "controllers.javascript.*;models.base.*;models.party.*;"
  )
  .settings(
    DockerBaseSettings("symbiotic-server") ++
      Seq(
        packageSummary in Docker := "Symbiotic Backend services",
        packageDescription in Docker := "Backend for the Symbiotic simple file management system",
        dockerExposedPorts in Docker := Seq(9000),
        dockerBaseImage in Docker := "openjdk:8"
      )
  )
  .settings(NoPublish)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      JBCrypt,
      PlayJson,
      PlayJsonJoda,
      PlayImport.ehcache,
      PlayImport.ws,
      PlayImport.guice,
      PlayImport.filters,
      PlayImport.evolutions,
      ScalaTest.scalaTestPlus % Test
    ) ++ Silhouette ++ Akka ++ Logback ++ PlaySlick
  )
  .dependsOn(coreLib, mongodb, postgres, json, playExtras)
  .dependsOn(testKit % Test)
