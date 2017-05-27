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
      scalaVersion := "2.11.11",
      scalacOptions := BaseScalacOpts,
      organization := "net.scalytica",
      scalacOptions in Test ++= Seq("-Yrangepos"),
      logBuffered in Test := false,
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
    val fullName = s"symbiotic-$name"

    Project(fullName, file(fullName))
      .settings(Settings.BaseSettings: _*)
      .settings(
        updateOptions := updateOptions.value.withCachedResolution(true)
      )
      .settings(resolvers ++= DependencyManagement.SymbioticResolvers)
      .settings(libraryDependencies ++= DependencyManagement.Specs2.Specs2Test)
      .settings(dependencyOverrides ++= DependencyManagement.Overrides)
  }

  object DependencyManagement {

    val SymbioticResolvers = Seq(
      Resolver.typesafeRepo("releases"),
      Resolver.sonatypeRepo("releases"),
      Resolver.bintrayRepo("scalaz", "releases"),
      Resolver.jcenterRepo
    )

    // Versions
    val AkkaVer: String        = "2.4.18"
    val CasbahVer: String      = "3.1.1"
    val FicusVer: String       = "1.4.0"
    val JBCryptVer: String     = "0.4"
    val JodaVer: String        = "2.9.9"
    val JodaConvertVer: String = "1.8.1"
    val LogbackVer: String     = "1.2.2"
    val PlaySlickVer: String   = "2.1.0"
    val PlayVer: String        = play.core.PlayVersion.current
    val PostgresVer: String    = "42.0.0"
    val Slf4jVer: String       = "1.7.25"
    val SilhouetteVer: String  = "4.0.0"
    val Specs2Ver: String      = "3.8.9"
    val ScalaGuiceVer: String  = "4.1.0"

    val Play: Seq[Def.Setting[_]] = PlaySettings.defaultSettings

    val PlayIteratees  = "com.typesafe.play" %% "play-iteratees" % PlayVer
    val PlayLogbackDep = "com.typesafe.play" %% "play-logback"   % PlayVer

    val Logback: Seq[ModuleID] = Seq[ModuleID](
      "ch.qos.logback" % "logback-core"    % LogbackVer,
      "ch.qos.logback" % "logback-classic" % LogbackVer
    )

    val Silhouette: Seq[ModuleID] = Seq[ModuleID](
      "com.mohiva" %% "play-silhouette"                 % SilhouetteVer,
      "com.mohiva" %% "play-silhouette-password-bcrypt" % SilhouetteVer,
      "com.mohiva" %% "play-silhouette-crypto-jca"      % SilhouetteVer,
      "com.mohiva" %% "play-silhouette-persistence"     % SilhouetteVer,
      "com.mohiva" %% "play-silhouette-testkit"         % SilhouetteVer % Test
    )

    val Akka: Seq[ModuleID] = Seq[ModuleID](
      "com.typesafe.akka" %% "akka-actor"  % AkkaVer,
      "com.typesafe.akka" %% "akka-stream" % AkkaVer,
      "com.typesafe.akka" %% "akka-slf4j"  % AkkaVer
    )

    val Slf4J       = "org.slf4j"      % "slf4j-api"    % Slf4jVer
    val JodaTime    = "joda-time"      % "joda-time"    % JodaVer
    val JBCrypt     = "org.mindrot"    % "jbcrypt"      % JBCryptVer
    val JodaConvert = "org.joda"       % "joda-convert" % JodaConvertVer
    val Postgres    = "org.postgresql" % "postgresql"   % PostgresVer

    val MongoDbDriver = Seq[ModuleID](
      "org.mongodb" %% "casbah-commons" % CasbahVer,
      "org.mongodb" %% "casbah-core"    % CasbahVer,
      "org.mongodb" %% "casbah-query"   % CasbahVer,
      "org.mongodb" %% "casbah-gridfs"  % CasbahVer
    )

    val IHeartFicus = "com.iheart"     %% "ficus"       % FicusVer
    val ScalaGuice  = "net.codingwell" %% "scala-guice" % ScalaGuiceVer

    object Specs2 {
      val core  = "org.specs2" %% "specs2-core"          % Specs2Ver
      val html  = "org.specs2" %% "specs2-html"          % Specs2Ver
      val junit = "org.specs2" %% "specs2-junit"         % Specs2Ver
      val extra = "org.specs2" %% "specs2-matcher-extra" % Specs2Ver

      val Specs2Test = Seq[ModuleID](
        core  % Test,
        html  % Test,
        junit % Test,
        extra % Test
      )

      val Specs2Compile = Seq[ModuleID](core, html, junit, extra)
    }

    val Overrides: Set[ModuleID] = Set[ModuleID](
      "com.typesafe.akka" %% "akka-actor"     % AkkaVer,
      "com.typesafe.akka" %% "akka-stream"    % AkkaVer,
      "com.typesafe.akka" %% "akka-slf4j"     % AkkaVer,
      "ch.qos.logback"    % "logback-core"    % LogbackVer,
      "ch.qos.logback"    % "logback-classic" % LogbackVer,
      "org.slf4j"         % "slf4j-api"       % Slf4jVer
    )

  }

}
