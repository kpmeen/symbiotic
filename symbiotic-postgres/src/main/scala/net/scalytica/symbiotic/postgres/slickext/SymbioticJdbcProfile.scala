package net.scalytica.symbiotic.postgres.slickext

import net.scalytica.symbiotic.api.types.Path
import slick.ast.Library.SqlOperator
import slick.jdbc.PostgresProfile

object ExtOperators {
  val ~ = new SqlOperator("~")
}

trait SymbioticJdbcProfile extends PostgresProfile {

  trait ExtraApi extends API {
    implicit def pathColumn(
        c: Rep[Path]
    ): PathExtensionMethods[Path] =
      new PathExtensionMethods[Path](c)

    implicit def pathOptColumn(
        c: Rep[Option[Path]]
    ): PathExtensionMethods[Option[Path]] =
      new PathExtensionMethods[Option[Path]](c)
  }

  override val api: ExtraApi = new ExtraApi {}

}

object SymbioticJdbcProfile extends SymbioticJdbcProfile
