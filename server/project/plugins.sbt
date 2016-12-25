resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" %% "sbt-plugin" % "2.5.10")

// Style checker
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// Plugin for pushing test coverage data to codacy
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.3.4")

// Use the Scalariform plugin to reformat the code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// I know this because SBT knows this...autogenerate BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.6.1")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.1.4")

