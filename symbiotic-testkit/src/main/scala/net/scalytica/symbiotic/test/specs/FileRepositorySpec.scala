package net.scalytica.symbiotic.test.specs

import net.scalytica.symbiotic.api.persistence.{
  FileRepository,
  FolderRepository
}
import net.scalytica.symbiotic.test.generators.TestUserId
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures

abstract class FileRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll {

  // scalastyle:off magic.number
  implicit val uid       = TestUserId.create()
  implicit val transform = (s: String) => TestUserId.asId(s)

  val fileRepo: FileRepository
  val folderRepo: FolderRepository

}
