import sbt._
import Setup.DependencyManagement._
import Setup.Settings

name := """symbiotic-server"""

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, BuildInfoPlugin)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      buildInfoBuildNumber
    ),
    buildInfoPackage := "net.scalytica.symbiotic.server",
    Settings.DockerSettings: _*,
    Settings.NoPublish: _*
  )

buildInfoOptions += BuildInfoOption.ToJson

PlayKeys.playOmnidoc := false

// Play router configuration
routesGenerator := InjectedRoutesGenerator

// Exclude generated stuff from coverage reports
coverageExcludedPackages :=
  "<empty>;router;controllers.Reverse*Controller;controllers.javascript.*;" +
    "models.base.*;models.party.*;"

// Dependency managment
libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  IHeartFicus,
  JBCrypt,
  ScalaGuice,
  Silhouette: _*,
  Akka: _*
)
