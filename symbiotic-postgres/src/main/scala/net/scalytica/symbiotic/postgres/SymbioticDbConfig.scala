package net.scalytica.symbiotic.postgres

import com.typesafe.config.Config
import net.scalytica.symbiotic.postgres.slickext.SymbioticJdbcProfile
import slick.basic.DatabaseConfig

trait SymbioticDbConfig {

  val config: Config

  val profile: SymbioticJdbcProfile

  lazy val dbConfig: DatabaseConfig[SymbioticJdbcProfile] =
    DatabaseConfig.forConfig[SymbioticJdbcProfile]("symbiotic.slick", config)

}

trait SymbioticDb extends SymbioticDbConfig {

  lazy val profile: SymbioticJdbcProfile = dbConfig.profile

  lazy val db = dbConfig.db

  lazy val dbSchema = Option(config.getString("symbiotic.postgres.schemaName"))
    .getOrElse(DefaultDmanSchema)
}
