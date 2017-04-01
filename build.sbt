name := """symbiotic"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).aggregate(
  client,
  server,
  coreLib,
  mongodb,
  postgres
)

lazy val coreLib  = project in file("symbiotic-core")
lazy val client   = project in file("symbiotic-web")
lazy val server   = (project in file("symbiotic-server")).dependsOn(coreLib)
lazy val mongodb  = (project in file("symbiotic-mongodb")).dependsOn(coreLib)
lazy val postgres = (project in file("symbiotic-postgres")).dependsOn(coreLib)

// Test options
scalacOptions in Test ++= Seq("-Yrangepos")
testOptions += Tests
  .Argument(TestFrameworks.Specs2, "html", "junitxml", "console")
