package net.scalytica.symbiotic.test.repositories

import akka.actor.ActorSystem
import akka.stream.Materializer
import net.scalytica.symbiotic.api.repository.IndexDataRepository
import net.scalytica.symbiotic.api.types.SymbioticContext

class TestIndexDataRepository extends IndexDataRepository {
  override def streamFiles()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ) = ???

  override def streamFolders()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ) = ???
}
