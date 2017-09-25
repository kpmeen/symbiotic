package net.scalytica.symbiotic.postgres.docmanagement

import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.{Path, SymbioticContext}
import net.scalytica.symbiotic.postgres.SymbioticDb
import play.api.libs.json.Json

trait SharedQueries { self: SymbioticDb with SymbioticDbTables =>

  import profile.api._

  protected def findLatestBaseQuery(
      baseQuery: FileQuery => FileQuery
  ): FileQuery = {
    val base    = baseQuery(filesTable)
    val grouped = filesTable.groupBy(_.fileId)

    for {
      f1 <- base
      f2 <- grouped.map(t => t._1 -> t._2.map(_.version).max)
      if f1.fileId === f2._1 && f1.version === f2._2
    } yield f1
  }

  protected def accessiblePartiesFilter(
      f: FileTable,
      ids: Seq[PartyId]
  ): Rep[Option[Boolean]] = {

    def cond(id: PartyId) =
      f.accessibleBy @> Json.parse(s"""[{"id": "${id.value}"}]""")

    ids match {
      case Nil          => Rep.None
      case head :: Nil  => cond(head).?
      case head :: tail => cond(head) || accessiblePartiesFilter(f, tail)
    }
  }

  protected def editableQuery(
      from: Path
  )(implicit ctx: SymbioticContext): FileQuery = {
    filesTable.filter { f =>
      f.ownerId === ctx.owner.id.value &&
      accessiblePartiesFilter(f, ctx.accessibleParties) &&
      f.isFolder === true &&
      f.isDeleted === false &&
      (f.path inSet from.allPaths)
    }
  }

}
