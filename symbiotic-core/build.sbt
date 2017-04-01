import sbt._

name := """symbiotic-core"""

lazy val root = project in file(".")


coverageExcludedPackages :=
  "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
    "net.scalytica.symbiotic.data.Implicits.*;"


