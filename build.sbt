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
  .settings(libraryDependencies += PlayImport.ws)
  .settings(libraryDependencies += ScalaGuice)
  .dependsOn(coreLib)

lazy val client = SymbioticProject("symbiotic-web")

lazy val server = SymbioticProject("symbiotic-server")
  .settings(DockerSettings)
  .settings(NoPublish)
  .settings(libraryDependencies ++= Silhouette)
  .settings(libraryDependencies ++= Akka)
  .settings(
    libraryDependencies ++= Seq(
      IHeartFicus,
      JBCrypt
    )
  )
  .dependsOn(coreLib, playExtras)
