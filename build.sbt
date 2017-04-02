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
  .settings(libraryDependencies += Slf4J)
  .settings(libraryDependencies += JodaTime)
  .settings(libraryDependencies += IHeartFicus)

lazy val mongodb = SymbioticProject("symbiotic-mongodb").dependsOn(coreLib)

lazy val postgres = SymbioticProject("symbiotic-postgres").dependsOn(coreLib)

lazy val playExtras = SymbioticProject("symbiotic-play")
  .settings(
    libraryDependencies ++= Seq(
      PlayImport.ws,
      PlayIteratees,
      ScalaGuice
    ) ++ Akka
  )
  .dependsOn(coreLib)

lazy val client =
  SymbioticProject("symbiotic-client")
    .enablePlugins(ScalaJSPlugin)
    .settings(NoPublish)

lazy val server = SymbioticProject("symbiotic-server")
  .enablePlugins(PlayScala, BuildInfoPlugin)
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
