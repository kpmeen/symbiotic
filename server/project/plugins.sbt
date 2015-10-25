resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// The Play plugin
addSbtPlugin("com.typesafe.play" %% "sbt-plugin" % "2.4.3")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

// Use the Scalariform plugin to reformat the code
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.5.1")

// Editor support
addSbtPlugin("org.ensime" % "ensime-sbt" % "0.2.0")
addSbtPlugin("com.typesafe.sbteclipse" %% "sbteclipse-plugin" % "4.0.0")

