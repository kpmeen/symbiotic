import sbt._

// Build script for the symbiotic API web server
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
    buildInfoPackage := "net.scalytica.symbiotic.server"
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
  filters
)
