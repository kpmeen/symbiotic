resolvers ++= DefaultOptions.resolvers(snapshot = false)
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.6")

addSbtPlugin("io.get-coursier" %% "sbt-coursier" % "1.0.0-RC12")

// Formatting and style checking
addSbtPlugin("com.geirsson"   % "sbt-scalafmt"           % "1.3.0")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Code coverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage"       % "1.5.1")
addSbtPlugin("com.codacy"    %% "sbt-codacy-coverage" % "1.3.11")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.1")

// Release management
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.1")

// scalajs-plugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")

// Misc useful plugins
addSbtPlugin("com.timushev.sbt" % "sbt-updates"   % "0.3.3")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo" % "0.7.0")
