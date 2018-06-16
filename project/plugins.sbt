resolvers ++= DefaultOptions.resolvers(snapshot = false)
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")
// Remove below resolver once the following issues has been resolved:
// https://issues.jboss.org/projects/JBINTER/issues/JBINTER-21
resolvers += "JBoss" at "https://repository.jboss.org/"

// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.15")

addSbtPlugin("io.get-coursier" %% "sbt-coursier" % "1.0.2")

// Formatting and style checking
addSbtPlugin("com.geirsson"   % "sbt-scalafmt"           % "1.5.1")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// Code coverage
addSbtPlugin("org.scoverage" %% "sbt-scoverage"       % "1.5.1")
addSbtPlugin("com.codacy"    %% "sbt-codacy-coverage" % "1.3.12")

// Native packaging plugin
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.3.3")

// Release management
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.8")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")

// scalajs-plugin
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")

// Misc useful plugins
addSbtPlugin("com.timushev.sbt" % "sbt-updates"   % "0.3.4")
addSbtPlugin("com.eed3si9n"     % "sbt-buildinfo" % "0.8.0")
