import com.typesafe.sbt.SbtNativePackager.autoImport._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import org.scalastyle.sbt.ScalastylePlugin.autoImport._
import sbt.Keys.{isSnapshot, _}
import sbt._

// scalastyle:off
object Settings {

  val BaseScalacOpts = Seq(
    "-encoding",
    "utf-8", // Specify character encoding used by source files.
    "-feature", // Emit warning and location for usages of features that should be imported explicitly.
    "-deprecation", // Emit warning and location for usages of deprecated APIs.
    "-unchecked", // Enable additional warnings where generated code depends on assumptions.
    "-explaintypes", // Explain type errors in more detail.
    "-Xfuture", // Turn on future language features.
    "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
    "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
    "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
    "-Xlint:option-implicit", // Option.apply used implicit view.
    "-Xlint:package-object-classes", // Class or object defined in package object.
    "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match", // Pattern match may not be typesafe.
    "-language:implicitConversions",
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds",
    "-language:existentials",
    "-language:postfixOps"
  )

  val ExperimentalScalacOpts = Seq(
    "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals", // Warn if a local definition is unused.
    "-Ywarn-unused:params", // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates" // Warn if a private member is unused.
    //    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  )

  val BaseSettings = Seq(
    organization := "net.scalytica",
    licenses += ("Apache-2.0", url(
      "http://opensource.org/licenses/https://opensource.org/licenses/Apache-2.0"
    )),
    maintainer := "Knut Petter Meen <kp@scalytica.net>",
    scalaVersion := Dependencies.Scala_2_12
  )

  val BaseSymbioticSettings = Seq(
    scalacOptions := BaseScalacOpts,
    scalacOptions in Test ++= Seq("-Yrangepos"),
    javaOptions += "-Duser.timezone=UTC", // Set timezone to UTC
    javaOptions in Test += "-Dlogger.resource=logback-test.xml",
    logBuffered in Test := false,
    fork in Test := true,
    testOptions += Tests
      .Argument(TestFrameworks.Specs2, "html", "junitxml", "console"),
    // Disable ScalaDoc
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq.empty,
    scalastyleFailOnWarning := true,
    scalastyleFailOnError := true
  )

  val GitlabRegistry = "registry.gitlab.com"
  val GitlabUser     = "kpmeen"

  val DockerBaseSettings = { moduleName: String =>
    Seq(
      maintainer in Docker := maintainer.value,
      dockerRepository := Some(s"$GitlabRegistry/$GitlabUser"),
      dockerAlias := {
        val tag = version.value
        DockerAlias(
          Some(GitlabRegistry),
          Some(GitlabUser),
          s"symbiotic/$moduleName",
          Some(tag)
        )
      },
      dockerUpdateLatest := {
        val snapshot = isSnapshot.value
        val log      = sLog.value
        if (!snapshot) {
          log.info("Building release, updating docker latest tag")
          true
        } else {
          log.info("Building SNAPSHOT, not updating latest tag")
          false
        }
      }
    )
  }

  val NoPublish = Seq(
    publish := {},
    publishLocal := {}
  )

  val BintrayPublish = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomExtra := <url>https://gitlab.com/kpmeen/symbiotic</url>
      <developers>
        <developer>
          <id>kpmeen</id>
          <name>Knut Petter Meen</name>
          <url>http://scalytica.net</url>
        </developer>
      </developers>
  )

  def SymbioticProject(name: String, base: Option[String] = None): Project = {
    val fullName = s"symbiotic-$name"
    val fullPath = base.map(b => s"$b/$fullName").getOrElse(fullName)

    Project(fullName, file(fullPath))
      .settings(BaseSettings: _*)
      .settings(BaseSymbioticSettings: _*)
      .settings(
        updateOptions := updateOptions.value.withCachedResolution(true)
      )
      .settings(resolvers ++= Dependencies.SymbioticResolvers)
      .settings(
        libraryDependencies ++= Seq(
          Dependencies.ScalaTest.scalaTest % Test,
          Dependencies.ScalaTest.scalactic % Test
        )
      )
      .settings(Dependencies.Overrides)
  }

}

// scalastyle:on
