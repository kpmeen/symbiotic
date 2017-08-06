package net.scalytica.symbiotic.api.repository

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

trait RepositoryProvider {

  implicit val actorSystem  = ActorSystem("symbiotic")
  implicit val materializer = ActorMaterializer()

  def fileRepository: FileRepository

  def folderRepository: FolderRepository

  def fsTreeRepository: FSTreeRepository

  def indexDataRepository: IndexDataRepository
}
