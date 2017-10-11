package net.scalytica.symbiotic.postgres

import com.typesafe.config.Config
import net.scalytica.symbiotic.postgres.slickext.SymbioticJdbcProfile
import slick.basic.DatabaseConfig

trait SymbioticDbConfig {

  def config: Config

  def profile: SymbioticJdbcProfile

  lazy val dbConfig: DatabaseConfig[SymbioticJdbcProfile] =
    DatabaseConfig.forConfig[SymbioticJdbcProfile](
      path = "symbiotic.persistence.slick.dbs.dman",
      config = config,
      classLoader = getClass.getClassLoader
    )

}

trait SymbioticDb extends SymbioticDbConfig {

  lazy val profile: SymbioticJdbcProfile = dbConfig.profile

  lazy val db = dbConfig.db

  lazy val dbSchema = Option(
    config.getString("symbiotic.persistence.postgres.schemaName")
  ).getOrElse(DefaultDmanSchema)
}
