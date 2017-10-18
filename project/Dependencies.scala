import org.scalajs.sbtplugin.JSModuleID
import play.sbt.PlaySettings
import sbt._

// scalastyle:off
object Dependencies {

  val SymbioticResolvers = Seq(
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("releases"),
    Resolver.bintrayRepo("scalaz", "releases"),
    Resolver.jcenterRepo
  )

  val ScalaVer    = "2.11.11"
  val ScalaFmtVer = "1.2.0"
  // Versions
  val AkkaVer: String          = "2.5.6"
  val CasbahVer: String        = "3.1.1"
  val FicusVer: String         = "1.4.2"
  val JBCryptVer: String       = "0.4"
  val JodaVer: String          = "2.9.9"
  val JodaConvertVer: String   = "1.8.1"
  val LogbackVer: String       = "1.2.3"
  val SlickVer: String         = "3.2.1"
  val PlaySlickVer: String     = "3.0.1"
  val SlickPgVer: String       = "0.15.3"
  val PlayVer: String          = play.core.PlayVersion.current
  val PostgresVer: String      = "42.1.3"
  val Slf4jVer: String         = "1.7.25"
  val SilhouetteVer: String    = "5.0.0"
  val ScalaTestVer: String     = "3.0.3"
  val ScalaTestPlusVer: String = "2.0.0"
  val ScalaGuiceVer: String    = "4.1.0"
  val Elastic4sVer: String     = "5.4.12"

  val Play: Seq[Def.Setting[_]] = PlaySettings.defaultSettings

  val PlayJson       = "com.typesafe.play" %% "play-json"      % PlayVer
  val PlayJsonJoda   = "com.typesafe.play" %% "play-json-joda" % PlayVer
  val PlayLogbackDep = "com.typesafe.play" %% "play-logback"   % PlayVer

  val PlaySlick: Seq[ModuleID] = Seq(
    "com.typesafe.play" %% "play-slick"            % PlaySlickVer,
    "com.typesafe.play" %% "play-slick-evolutions" % PlaySlickVer
  )

  val Slick: Seq[ModuleID] = Seq(
    "com.typesafe.slick" %% "slick"          % SlickVer,
    "com.typesafe.slick" %% "slick-hikaricp" % SlickVer
  )

  val SlickPg: Seq[ModuleID] = Seq(
    "com.github.tminglei" %% "slick-pg"           % SlickPgVer,
    "com.github.tminglei" %% "slick-pg_play-json" % SlickPgVer exclude ("com.typesafe.play", "play-json_2.11")
  )

  val Logback: Seq[ModuleID] = Seq[ModuleID](
    "ch.qos.logback" % "logback-core"    % LogbackVer,
    "ch.qos.logback" % "logback-classic" % LogbackVer
  )

  val ApacheLog4j: Seq[ModuleID] = Seq(
    "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2",
    "org.apache.logging.log4j" % "log4j-api"      % "2.8.2",
    "org.apache.logging.log4j" % "log4j-core"     % "2.8.2"
  )

  val Silhouette: Seq[ModuleID] = Seq[ModuleID](
    "com.mohiva" %% "play-silhouette"                 % SilhouetteVer,
    "com.mohiva" %% "play-silhouette-password-bcrypt" % SilhouetteVer,
    "com.mohiva" %% "play-silhouette-crypto-jca"      % SilhouetteVer,
    "com.mohiva" %% "play-silhouette-persistence"     % SilhouetteVer,
    "com.mohiva" %% "play-silhouette-testkit"         % SilhouetteVer % Test
  )

  val AkkaTestKits = (scope: Configuration) =>
    Seq[ModuleID](
      "com.typesafe.akka" %% "akka-testkit"        % AkkaVer % scope,
      "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVer % scope
  )

  val Akka: Seq[ModuleID] = Seq[ModuleID](
    "com.typesafe.akka" %% "akka-actor"  % AkkaVer,
    "com.typesafe.akka" %% "akka-stream" % AkkaVer,
    "com.typesafe.akka" %% "akka-slf4j"  % AkkaVer
  ) ++ AkkaTestKits(Test)

  val Slf4j       = "org.slf4j"      % "slf4j-api"    % Slf4jVer
  val Slf4jNop    = "org.slf4j"      % "slf4j-nop"    % Slf4jVer
  val JodaTime    = "joda-time"      % "joda-time"    % JodaVer
  val JodaConvert = "org.joda"       % "joda-convert" % JodaConvertVer
  val JBCrypt     = "org.mindrot"    % "jbcrypt"      % JBCryptVer
  val Postgres    = "org.postgresql" % "postgresql"   % PostgresVer
  val IHeartFicus = "com.iheart"     %% "ficus"       % FicusVer
  val ScalaGuice  = "net.codingwell" %% "scala-guice" % ScalaGuiceVer exclude ("com.google.guava", "guava")

  val Elastic4s = Seq[ModuleID](
    "com.sksamuel.elastic4s" %% "elastic4s-core"         % Elastic4sVer,
    "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % Elastic4sVer,
    "com.sksamuel.elastic4s" %% "elastic4s-play-json"    % Elastic4sVer
  )

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
    "org.scala-lang"     % "scala-library"    % ScalaVer,
    "com.typesafe"       %% "ssl-config-core" % "0.2.2",
    "com.typesafe"       % "config"           % "1.3.1",
    "com.typesafe.akka"  %% "akka-actor"      % AkkaVer,
    "com.typesafe.akka"  %% "akka-stream"     % AkkaVer,
    "com.typesafe.akka"  %% "akka-slf4j"      % AkkaVer,
    "joda-time"          % "joda-time"        % JodaVer,
    "org.joda"           % "joda-convert"     % JodaConvertVer,
    "org.postgresql"     % "postgresql"       % PostgresVer,
    "com.typesafe.slick" %% "slick"           % SlickVer,
    "ch.qos.logback"     % "logback-core"     % LogbackVer,
    "ch.qos.logback"     % "logback-classic"  % LogbackVer,
    "org.slf4j"          % "slf4j-api"        % Slf4jVer
  )

  object ClientDependencies {

    import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
    import sbt.Keys._

    val scalaJSReactVersion = "0.11.3"
    val scalaCssVersion     = "0.5.1"
    val scalazVersion       = "7.2.7"
    val monocleVersion      = "1.3.2"
    val reactJsVersion      = "15.3.2"

    val settings = Seq(
      libraryDependencies ++= Seq(
        compilerPlugin(
          "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
        ),
        "be.doeraene"                                      %%% "scalajs-jquery" % "0.9.1",
        "com.typesafe.play"                                %%% "play-json" % "2.6.3",
        "com.github.japgolly.scalajs-react"                %%% "core" % scalaJSReactVersion,
        "com.github.japgolly.scalajs-react"                %%% "extra" % scalaJSReactVersion,
        "com.github.japgolly.scalajs-react"                %%% "ext-scalaz72" % scalaJSReactVersion,
        "com.github.japgolly.scalajs-react"                %%% "ext-monocle" % scalaJSReactVersion,
        "com.github.japgolly.scalacss"                     %%% "core" % scalaCssVersion,
        "com.github.japgolly.scalacss"                     %%% "ext-react" % scalaCssVersion,
        "com.github.julien-truffaut" %%%! s"monocle-core"  % monocleVersion,
        "com.github.julien-truffaut" %%%! s"monocle-macro" % monocleVersion
      ),
      jsDependencies ++= Seq(
        "org.webjars.bower" % "react"          % reactJsVersion / "react-with-addons.js" minified "react-with-addons.min.js" commonJSName "React",
        "org.webjars.bower" % "react"          % reactJsVersion / "react-dom.js" minified "react-dom.min.js" dependsOn "react-with-addons.js" commonJSName "ReactDOM",
        "org.webjars.bower" % "react"          % reactJsVersion / "react-dom-server.js" minified "react-dom-server.min.js" dependsOn "react-dom.js" commonJSName "ReactDOMServer",
        "org.webjars"       % "log4javascript" % "1.4.10" / "js/log4javascript.js"
      )
    )
  }

}

// scalastyle:on
