package net.scalytica.symbiotic.api.repository

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Merge, Source}
import net.scalytica.symbiotic.api.types.{
  File,
  Folder,
  ManagedFile,
  SymbioticContext
}

trait IndexDataRepository {

  def streamFiles()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[File, NotUsed]

  def streamFolders()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[Folder, NotUsed]

  def streamAll()(
      implicit ctx: SymbioticContext,
      as: ActorSystem,
      mat: Materializer
  ): Source[ManagedFile, NotUsed] = {
    // Initialize a cursor on the collection to feed into a stream Source.
    Source.combine(streamFolders(), streamFiles())(s => Merge[ManagedFile](s))
  }
}
