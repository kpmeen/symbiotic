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
  client,
  server
)

lazy val sharedLib = SymbioticProject("shared")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayIteratees,
      Slf4J,
      JodaTime,
      JodaConvert
    ) ++ Akka
  )
  .settings(
    coverageExcludedPackages :=
      "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
        "net.scalytica.symbiotic.data.Implicits.*;"
  )

lazy val coreLib = SymbioticProject("core")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies += IHeartFicus)
  .dependsOn(sharedLib)
  .dependsOn(mongodb % Test)

lazy val mongodb = SymbioticProject("mongodb")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(libraryDependencies += IHeartFicus)
  .settings(libraryDependencies ++= MongoDbDriver)
  .dependsOn(sharedLib)

lazy val postgres = SymbioticProject("postgres")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .dependsOn(sharedLib)

lazy val playExtras = SymbioticProject("play")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      ScalaGuice
    )
  )
  .dependsOn(sharedLib)

lazy val client =
  SymbioticProject("client").enablePlugins(ScalaJSPlugin).settings(NoPublish)

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
      PlayImport.cache,
      PlayImport.ws,
      PlayImport.filters
    ) ++ Silhouette ++ Akka
  )
  .dependsOn(coreLib, playExtras, mongodb)
