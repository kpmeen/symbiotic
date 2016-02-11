resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" %% "sbt-plugin" % "2.4.6")

// Style checker
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

// Plugin for pushing test coverage data to codacy
addSbtPlugin("com.codacy" % "sbt-codacy-coverage" % "1.2.1")

// Use the Scalariform plugin to reformat the code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.5.1")

// I know this because SBT knows this...autogenerate BuildInfo for the project
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")