// Build script for the symbiotic API web server
name := """symbiotic-server"""

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
