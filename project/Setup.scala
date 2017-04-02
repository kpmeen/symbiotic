import java.net.URL

import com.typesafe.sbt.SbtNativePackager.autoImport.{
  maintainer,
  packageDescription,
  packageSummary
}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import play.sbt.PlaySettings
import sbt.Keys._
import sbt.{Def, _}

object Setup {

  object Settings {
    val BaseScalacOpts = Seq(
      "-feature", // Emit warning and location for usages of features that should be imported explicitly.
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-Xlint", // Enable recommended additional warnings.
      "-Xfatal-warnings", // Fail the compilation if there are any warnings.
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    )

    val ExtraScalacOpts = Seq(
      "-unchecked", // Enable additional warnings where generated code depends on assumptions.
      "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
      "-Ywarn-dead-code", // Warn when dead code is identified.
      "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
      "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
      "-Ywarn-numeric-widen" // Warn when numerics are widened.
    )

    val BaseSettings = Seq(
      version := "1.0-SNAPSHOT",
      scalaVersion := "2.11.8",
      scalacOptions := BaseScalacOpts,
      organization := "net.scalytica",
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
    val AkkaVersion: String        = "2.4.17"
    val CasbahVersion: String      = "3.1.1"
    val LogbackVersion: String     = "1.2.2"
    val PlaySlickVersion: String   = "2.1.0"
    val SilhouetteVersion: String  = "4.0.0"
    val Slf4jVersion: String       = "1.7.25"
    val Specs2Version: String      = "3.8.9"
    val JBCryptVersion: String     = "0.3m"
    val FicusVersion: String       = "1.4.0"
    val ScalaGuiceVersion: String  = "4.1.0"
    val JodaVersion: String        = "2.9.9"
    val JodaConvertVersion: String = "1.8.1"
    val PlayVersion: String        = play.core.PlayVersion.current

    val Play: Seq[Def.Setting[_]] = PlaySettings.defaultSettings
    val PlayIteratees
      : ModuleID = "com.typesafe.play" %% "play-iteratees" % PlayVersion

    val Logback: Seq[ModuleID] = Seq[ModuleID](
      "ch.qos.logback" % "logback-core"    % LogbackVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion
    )

    val Slf4J: ModuleID = "org.slf4j" % "slf4j-api" % Slf4jVersion

    val Silhouette: Seq[ModuleID] = Seq[ModuleID](
      "com.mohiva" %% "play-silhouette"                 % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-password-bcrypt" % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-crypto-jca"      % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-persistence"     % SilhouetteVersion,
      "com.mohiva" %% "play-silhouette-testkit"         % SilhouetteVersion % Test
    )

    val Akka: Seq[ModuleID] = Seq[ModuleID](
      "com.typesafe.akka" %% "akka-actor"  % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"  % AkkaVersion
    )

    val JodaTime: ModuleID      = "joda-time"   % "joda-time" % JodaVersion
    val MongoDbDriver: ModuleID = "org.mongodb" %% "casbah"   % CasbahVersion
    val JBCrypt: ModuleID       = "org.mindrot" % "jbcrypt"   % JBCryptVersion
    val IHeartFicus: ModuleID   = "com.iheart"  %% "ficus"    % FicusVersion
    // format: off
    // scalastyle: off
    val JodaConvert: ModuleID   = "org.joda"    % "joda-convert" % JodaConvertVersion
    val ScalaGuice: ModuleID = "net.codingwell" %% "scala-guice" % ScalaGuiceVersion
    // format: on
    // scalastyle:on

    val Specs2: Seq[ModuleID] = Seq[ModuleID](
      "org.specs2" %% "specs2-core"          % Specs2Version % Test,
      "org.specs2" %% "specs2-html"          % Specs2Version % Test,
      "org.specs2" %% "specs2-junit"         % Specs2Version % Test,
      "org.specs2" %% "specs2-matcher-extra" % Specs2Version % Test
    )

    val Overrides: Set[ModuleID] = Set[ModuleID](
      "com.typesafe.akka" %% "akka-actor"     % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream"    % AkkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"     % AkkaVersion,
      "ch.qos.logback"    % "logback-core"    % LogbackVersion,
      "ch.qos.logback"    % "logback-classic" % LogbackVersion,
      "org.slf4j"         % "slf4j-api"       % Slf4jVersion
    )

  }

}
