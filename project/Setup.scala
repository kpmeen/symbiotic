import com.typesafe.sbt.SbtNativePackager.autoImport.{
  maintainer,
  packageDescription,
  packageSummary
}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import play.sbt.PlaySettings
import sbt.Keys._
import sbt.{Def, _}

// scalastyle:off
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
      javaOptions in Test += "-Dlogger.resource=logback-test.xml",
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
      .settings(
        libraryDependencies ++= Seq(
          DependencyManagement.ScalaTest.scalaTest % Test,
          DependencyManagement.ScalaTest.scalactic % Test
        )
      )
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
    val AkkaVer: String          = "2.4.19"
    val CasbahVer: String        = "3.1.1"
    val FicusVer: String         = "1.4.1"
    val JBCryptVer: String       = "0.4"
    val JodaVer: String          = "2.9.9"
    val JodaConvertVer: String   = "1.8.1"
    val LogbackVer: String       = "1.2.3"
    val SlickVer: String         = "3.2.0"
    val PlaySlickVer: String     = "2.1.0"
    val PlayVer: String          = play.core.PlayVersion.current
    val PostgresVer: String      = "42.1.1"
    val Slf4jVer: String         = "1.7.25"
    val SilhouetteVer: String    = "4.0.0"
    val ScalaTestVer: String     = "3.0.3"
    val ScalaTestPlusVer: String = "2.0.0"
    val ScalaGuiceVer: String    = "4.1.0"

    val Play: Seq[Def.Setting[_]] = PlaySettings.defaultSettings

    val PlayIteratees  = "com.typesafe.play" %% "play-iteratees" % PlayVer
    val PlayLogbackDep = "com.typesafe.play" %% "play-logback"   % PlayVer

    val PlaySlick: Seq[ModuleID] = Seq(
      "com.typesafe.play" %% "play-slick"            % PlaySlickVer,
      "com.typesafe.play" %% "play-slick-evolutions" % PlaySlickVer
    )

    val Slick: Seq[ModuleID] = Seq(
      "com.typesafe.slick" %% "slick"          % SlickVer,
      "com.typesafe.slick" %% "slick-hikaricp" % SlickVer
    )

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
      "com.typesafe.akka" %% "akka-actor"          % AkkaVer,
      "com.typesafe.akka" %% "akka-stream"         % AkkaVer,
      "com.typesafe.akka" %% "akka-slf4j"          % AkkaVer,
      "com.typesafe.akka" %% "akka-testkit"        % AkkaVer % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVer % Test
    )

    val Slf4j       = "org.slf4j"      % "slf4j-api"    % Slf4jVer
    val Slf4jNop    = "org.slf4j"      % "slf4j-nop"    % Slf4jVer
    val JodaTime    = "joda-time"      % "joda-time"    % JodaVer
    val JodaConvert = "org.joda"       % "joda-convert" % JodaConvertVer
    val JBCrypt     = "org.mindrot"    % "jbcrypt"      % JBCryptVer
    val Postgres    = "org.postgresql" % "postgresql"   % PostgresVer
    val IHeartFicus = "com.iheart"     %% "ficus"       % FicusVer
    val ScalaGuice  = "net.codingwell" %% "scala-guice" % ScalaGuiceVer

    val MongoDbDriver = Seq[ModuleID](
      "org.mongodb" %% "casbah-commons" % CasbahVer,
      "org.mongodb" %% "casbah-core"    % CasbahVer,
      "org.mongodb" %% "casbah-query"   % CasbahVer,
      "org.mongodb" %% "casbah-gridfs"  % CasbahVer
    )

    object ScalaTest {
      val scalaTest     = "org.scalatest"          %% "scalatest"          % ScalaTestVer
      val scalactic     = "org.scalactic"          %% "scalactic"          % ScalaTestVer
      val scalaTestPlus = "org.scalatestplus.play" %% "scalatestplus-play" % ScalaTestPlusVer
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
// scalastyle:on
