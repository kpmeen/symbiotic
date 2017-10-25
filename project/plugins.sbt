resolvers ++= DefaultOptions.resolvers(snapshot = false)
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.6")

addSbtPlugin("io.get-coursier" %% "sbt-coursier" % "1.0.0-RC12")

// Formatting and style checking
libraryDependencies += "com.geirsson" %% "scalafmt-bootstrap" % "0.6.6"

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.9.0")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// Plugin for pushing test coverage data to codacy
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.11")

// I know this because SBT knows this...autogenerate BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.3")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.1")

// scalajs-plugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")

// Release management
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")
