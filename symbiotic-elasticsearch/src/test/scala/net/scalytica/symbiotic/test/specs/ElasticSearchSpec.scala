package net.scalytica.symbiotic.test.specs

import org.scalatest.EitherValues
import play.api.Configuration

trait ElasticSearchSpec extends PersistenceSpec with EitherValues {

  val esHost = sys.props
    .get("CI")
    .orElse(sys.env.get("CI"))
    .map(_ => "elasticsearch")
    .getOrElse("localhost")

  // scalastyle:off
  val esConfig =
    Configuration(
      "symbiotic.index.elasticsearch.host" -> esHost,
      "akka.loggers"                       -> Seq("akka.event.slf4j.Slf4jLogger"),
      "akka.loglevel"                      -> "DEBUG",
      "akka.logging-filter"                -> "akka.event.slf4j.Slf4jLoggingFilter"
    )
  // scalastyle:on

  override lazy val config = (configuration ++ esConfig).underlying

}
