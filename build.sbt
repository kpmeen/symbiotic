import sbt._
import Setup.SymbioticProject
import Setup.Settings._
import Setup.DependencyManagement._
import play.sbt.PlayImport

name := """symbiotic"""

lazy val root = (project in file(".")).aggregate(
  coreLib,
  mongodb,
  postgres,
  playExtras,
  client,
  server
)

lazy val coreLib = SymbioticProject("symbiotic-core")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayIteratees,
      Slf4J,
      JodaTime,
      JodaConvert,
      IHeartFicus
    ) ++ Akka
  )

lazy val mongodb = SymbioticProject("symbiotic-mongodb")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      MongoDbDriver
    )
  )
  .dependsOn(coreLib)

lazy val postgres = SymbioticProject("symbiotic-postgres")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .dependsOn(coreLib)

lazy val playExtras = SymbioticProject("symbiotic-play")
  .settings(scalacOptions ++= ExtraScalacOpts)
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      ScalaGuice
    )
  )
  .dependsOn(coreLib)

lazy val client =
  SymbioticProject("symbiotic-client")
    .enablePlugins(ScalaJSPlugin)
    .settings(NoPublish)

lazy val server = SymbioticProject("symbiotic-server")
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
    buildInfoPackage := "net.scalytica.symbiotic.server"
  )
  .settings(DockerSettings)
  .settings(NoPublish)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      JBCrypt
    ) ++ Silhouette ++ Akka
  )
  .dependsOn(coreLib, playExtras)
