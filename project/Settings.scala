import com.typesafe.sbt.SbtNativePackager.autoImport.{
  maintainer,
  packageDescription,
  packageSummary
}
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import sbt.Keys._
import sbt._

// scalastyle:off
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
    organization := "net.scalytica",
    licenses += ("Apache-2.0", url(
      "http://opensource.org/licenses/https://opensource.org/licenses/Apache-2.0"
    )),
    maintainer := "Knut Petter Meen <kp@scalytica.net>",
    scalaVersion := Dependencies.Scala_2_12,
    crossScalaVersions := Seq(Dependencies.Scala_2_12, Dependencies.Scala_2_11)
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
    // Disable scaladoc
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Seq.empty
  )

  val GitlabRegistry = "registry.gitlab.com"
  val GitlabUser     = "kpmeen"

  val DockerBaseSettings = (moduleName: String) =>
    Seq(
      maintainer in Docker := maintainer.value,
      dockerRepository := Some(s"$GitlabRegistry/$GitlabUser"),
      dockerAlias := DockerAlias(
        Some(GitlabRegistry),
        Some(GitlabUser),
        moduleName,
        Some("latest")
      )
  )

  val DockerBackendSettings = (moduleName: String) =>
    DockerBaseSettings(moduleName) ++
      Seq(
        packageSummary in Docker := "Symbiotic Backend services",
        packageDescription in Docker := "Backend for the Symbiotic simple file management system",
        dockerExposedPorts in Docker := Seq(9000),
        dockerBaseImage in Docker := "openjdk:8"
    )

  val NoPublish = Seq(
    publish := {},
    publishLocal := {}
  )

  val BintrayPublish = Seq(
    publishMavenStyle := true,
    publishArtifact in Test := false,
    pomExtra := (
      <url>https://gitlab.com/kpmeen/symbiotic</url>
        <developers>
          <developer>
            <id>kpmeen</id>
            <name>Knut Petter Meen</name>
            <url>http://scalytica.net</url>
          </developer>
        </developers>
    )
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
