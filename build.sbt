import Dependencies._
import Settings._
import play.sbt.PlayImport
import sbt._

name := """symbiotic"""

lazy val root = (project in file("."))
  .settings(NoPublish)
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
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      JodaTime,
      JodaConvert,
      ScalaTest.scalaTest % Test,
      ScalaTest.scalactic % Test
    ) ++ Akka
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .settings(
    coverageExcludedPackages :=
      "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
        "net.scalytica.symbiotic.data.Implicits.*;"
  )
  .settings(BintrayPublish: _*)

lazy val fsLib = SymbioticProject("fs")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(IHeartFicus) ++ Akka
  )
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .settings(BintrayPublish: _*)

lazy val coreLib = SymbioticProject("core")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies += IHeartFicus)
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test, mongodb % Test, postgres % Test)
  .settings(BintrayPublish: _*)

lazy val json = SymbioticProject("json")
  .settings(scalacOptions ++= ExtraScalacOpts)
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
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies ++= Seq(IHeartFicus) ++ Akka ++ MongoDbDriver)
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test)
  .settings(BintrayPublish: _*)

lazy val postgres = SymbioticProject("postgres")
  .settings(scalacOptions ++= ExtraScalacOpts)
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
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies ++= Seq(IHeartFicus) ++ Elastic4s)
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(sharedLib, json)
  .dependsOn(testKit % Test, mongodb % Test, postgres % Test)
  .settings(BintrayPublish: _*)

lazy val playExtras = SymbioticProject("play")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies ++= Seq(PlayImport.ws, ScalaGuice))
  .settings(libraryDependencies ++= Logback.map(_ % Test))
  .dependsOn(coreLib)
  .dependsOn(testKit % Test)
  .settings(BintrayPublish: _*)

lazy val testKit = SymbioticProject("testkit")
  .settings(scalacOptions ++= ExtraScalacOpts)
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
  SymbioticProject("client")
    .enablePlugins(ScalaJSPlugin)
    .settings(fork in Test := false)
    .settings(NoPublish)

lazy val server = SymbioticProject("server")
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
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
  .settings(routesGenerator := InjectedRoutesGenerator)
  .settings(
    coverageExcludedPackages :=
      "<empty>;router;controllers.Reverse*Controller;" +
        "controllers.javascript.*;models.base.*;models.party.*;"
  )
  .settings(DockerSettings)
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
      ScalaTest.scalaTestPlus % Test
    ) ++ Silhouette ++ Akka ++ Logback
  )
  .dependsOn(coreLib, json, playExtras, mongodb)
  .dependsOn(testKit % Test)
