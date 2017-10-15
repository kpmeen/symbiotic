package net.scalytica.symbiotic.config

import com.typesafe.config.{Config, ConfigFactory}

object ConfigReader {

  def load(): Config = {
    sys.props
      .get("config.resource")
      .map(cfg => ConfigFactory.load(cfg))
      .orElse {
        sys.props
          .get("config.file")
          .map(file => ConfigFactory.parseFile(new java.io.File(file)))
      }
      .getOrElse(ConfigFactory.load())
  }

}
