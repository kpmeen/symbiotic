import sbt._

name := """symbiotic-core"""

coverageExcludedPackages :=
  "<empty>;net.scalytica.symbiotic.data.MetadataKeys.*;" +
    "net.scalytica.symbiotic.data.Implicits.*;"