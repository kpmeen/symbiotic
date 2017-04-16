resolvers += Resolver.typesafeRepo("releases")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.13")

addSbtPlugin("io.get-coursier" %% "sbt-coursier" % "1.0.0-RC1")

// Formatting and style checking
addSbtPlugin("com.geirsson"   % "sbt-scalafmt"           % "0.6.3")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// Plugin for pushing test coverage data to codacy
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.8")

// I know this because SBT knows this...autogenerate BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.2.0-M8")

// scalajs-plugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.14")
