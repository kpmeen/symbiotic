import java.net.URL

import com.typesafe.sbt.SbtNativePackager.autoImport.{
  maintainer,
  packageDescription,
  packageSummary
}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import play.sbt.PlaySettings
import sbt.Keys._
import sbt._

object Setup {

  object Settings {
    val BaseSettings = Seq(
      organization := "net.scalytica",
      scalacOptions := Seq(
        """-deprecation""", // Emit warning and location for usages of deprecated APIs.
        "-feature", // Emit warning and location for usages of features that should be imported explicitly.
        "-unchecked", // Enable additional warnings where generated code depends on assumptions.
        "-Xfatal-warnings", // Fail the compilation if there are any warnings.
        "-Xlint", // Enable recommended additional warnings.
        "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
        "-Ywarn-dead-code", // Warn when dead code is identified.
        "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
        "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
        "-Ywarn-numeric-widen", // Warn when numerics are widened.
        "-language:implicitConversions",
        "-language:higherKinds",
        "-language:existentials",
        "-language:postfixOps"
      ),
      scalacOptions in Test ++= Seq("-Yrangepos"),
      javacOptions += "-Xlint:deprecation",
      testOptions += Tests
        .Argument(TestFrameworks.Specs2, "html", "junitxml", "console")
    )

    val GitlabRegistry = "registry.gitlab.com"
    val GitlabUser     = "kpmeen"

    val DockerSettings = Seq(
      maintainer in Docker := "Knut Petter Meen <kp@scalytica.net>",
      packageSummary in Docker := "Symbiotic Backend services",
      packageDescription in Docker := "Backend for the Symbiotic simple file management system",
      dockerExposedPorts in Docker := Seq(9000),
      dockerBaseImage in Docker := "openjdk:8",
      dockerRepository := Some(s"$GitlabRegistry/$GitlabUser"),
      dockerAlias := DockerAlias(
        Some(GitlabRegistry),
        Some(GitlabUser),
        "symbiotic",
        Some("latest")
      )
    )

    val NoPublish = Seq(
      publish := {},
      publishLocal := {}
    )

  }

  def SymbioticProject(name: String): Project = {
    Project(name, file(name))
      .settings(Settings.BaseSettings: _*)
      .settings(
        updateOptions := updateOptions.value.withCachedResolution(true)
      )
      .settings(resolvers ++= DependencyManagement.SymbioticResolvers)
      .settings(dependencyOverrides ++= DependencyManagement.Overrides)
  }

  object DependencyManagement {

    val SymbioticResolvers = Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("snapshots"),
      Resolver.bintrayRepo("scalaz", "releases"),
      Resolver.jcenterRepo,
      Resolver.url(
        name = "Atlassian Releases",
        baseURL = new URL("https://maven.atlassian.com/public/")
      )
    )

    // Versions
    val AkkaVersion       = "2.4.17"
    val CasbahVersion     = "3.1.1"
    val LogbackVersion    = "1.2.2"
    val PlaySlickVersion  = "2.1.0"
    val SilhouetteVersion = "4.0.0"
    val Slf4jVersion      = "1.7.25"
    val Specs2Version     = "3.8.9"
    val JBCryptVersion    = "0.3m"
    val FicusVersion      = "1.4.0"
    val ScalaGuiceVersion = "4.1.0"

    val Play = PlaySettings.defaultSettings

    val Silhouette = Seq(
      "com.mohiva" %% "play-silhouette"                 % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-password-bcrypt" % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-crypto-jca"      % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-persistence"     % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-testkit"         % SilhouetteVersion % Test
    )

    val Akka = Seq(
      "com.typesafe.akka" %% "akka-actor"  % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"  % AkkaVersion
    )

    val JBCrypt     = "org.mindrot"    % "jbcrypt"      % JBCryptVersion
    val IHeartFicus = "com.iheart"     %% "ficus"       % FicusVersion
    val ScalaGuice  = "net.codingwell" %% "scala-guice" % ScalaGuiceVersion

    val Specs2 = Seq(
      "org.specs2" %% "specs2-core"          % Specs2Version % Test,
      "org.specs2" %% "specs2-html"          % Specs2Version % Test,
      "org.specs2" %% "specs2-junit"         % Specs2Version % Test,
      "org.specs2" %% "specs2-matcher-extra" % Specs2Version % Test
    )

    val Overrides = Set(
      "com.typesafe.akka" %% "akka-actor"     % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream"    % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"     % AkkaVersion,
      "ch.qos.logback"    % "logback-core"    % LogbackVersion,
      "ch.qos.logback"    % "logback-classic" % LogbackVersion,
      "org.slf4j"         % "slf4j-api"       % Slf4jVersion
    )

  }
}
