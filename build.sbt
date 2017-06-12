import Setup.DependencyManagement._
import Setup.Settings._
import Setup.SymbioticProject
import play.sbt.PlayImport
import sbt._

name := """symbiotic"""

lazy val root = (project in file(".")).aggregate(
  sharedLib,
  coreLib,
  mongodb,
  postgres,
  playExtras,
  testKit,
  server
)

lazy val sharedLib = SymbioticProject("shared")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayIteratees,
      JodaTime,
      JodaConvert,
      Slf4jNop            % Test,
      ScalaTest.scalaTest % Test,
      ScalaTest.scalactic % Test
    ) ++ Akka
  )
  .settings(
    coverageExcludedPackages :=
      "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
        "net.scalytica.symbiotic.data.Implicits.*;"
  )

lazy val coreLib = SymbioticProject("core")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(fork in Test := true)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      Slf4jNop       % Test,
      PlayLogbackDep % Test
    )
  )
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test, mongodb % Test, postgres % Test)

lazy val mongodb = SymbioticProject("mongodb")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(fork in Test := true)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      Slf4jNop % Test
    ) ++ MongoDbDriver
  )
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test)

lazy val postgres = SymbioticProject("postgres")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(fork in Test := true)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      Postgres,
      Slf4jNop % Test
    ) ++ Slick
  )
  .dependsOn(sharedLib)
  .dependsOn(testKit % Test)

lazy val playExtras = SymbioticProject("play")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      ScalaGuice
    )
  )
  .dependsOn(coreLib)
  .dependsOn(testKit % Test)

lazy val testKit = SymbioticProject("testkit")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      ScalaTest.scalaTest,
      ScalaTest.scalactic,
      Postgres
    ) ++ MongoDbDriver
  )
  .dependsOn(sharedLib)

lazy val client =
  SymbioticProject("client").enablePlugins(ScalaJSPlugin).settings(NoPublish)

lazy val server = SymbioticProject("server")
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(fork in Test := true)
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
      PlayImport.cache,
      PlayImport.ws,
      PlayImport.json,
      PlayImport.filters,
      ScalaTest.scalaTestPlus % Test
    ) ++ Silhouette ++ Akka ++ Logback
  )
  .dependsOn(coreLib, playExtras, mongodb)
  .dependsOn(testKit % Test)
