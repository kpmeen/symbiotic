package net.scalytica.symbiotic.postgres.docmanagement

import net.scalytica.symbiotic.postgres.SymbioticDb

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

}
