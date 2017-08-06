package net.scalytica.symbiotic.test.specs

import net.scalytica.symbiotic.api.repository.{
  FSTreeRepository,
  FolderRepository
}
import net.scalytica.symbiotic.api.types.Path
import net.scalytica.symbiotic.api.types.ResourceOwner.{OrgOwner, Owner}
import net.scalytica.symbiotic.test.generators.{
  FolderGenerator,
  TestContext,
  TestOrgId,
  TestUserId
}
import org.scalatest.Inspectors.forAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class FSTreeRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll {

  // scalastyle:off magic.number
  val usrId = TestUserId.create()
  val orgId = TestOrgId.create()
  val owner = Owner(orgId, OrgOwner)

  implicit val ctx = TestContext(usrId, owner)

  def folderRepo: FolderRepository
  def fstreeRepo: FSTreeRepository

  val folders = {
    FolderGenerator.createFolders(
      owner = orgId,
      baseName = "fstreefolderA",
      depth = 15
    ) ++ FolderGenerator.createFolders(
      owner = orgId,
      baseName = "fstreefolderB",
      depth = 5
    ) ++ FolderGenerator.createFolders(
      owner = orgId,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderC",
      depth = 11
    ) ++ FolderGenerator.createFolders(
      owner = orgId,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderD",
      depth = 9
    )
  }

  "The FS tree repository" should {

    "contain managed files" in {
      val res = Future
        .sequence(folders.map(f => folderRepo.save(f)))
        .futureValue
        .flatten
        .size mustBe 40
    }

    "return all file ids and their paths" in {
      val fromPath =
        Path.root.append("fstreefolderB_1").append("fstreefolderB_2")
      val res = fstreeRepo.treePaths(Option(fromPath)).futureValue
      res.size mustBe 4
      forAll(res.zip(2 to 5)) { r =>
        r._1._2.nameOfLast mustBe s"fstreefolderB_${r._2}"
      }
    }

    "return all managed files for the subtree of a given path" in {
      val fromPath =
        Path.root.append("fstreefolderA_1").append("fstreefolderA_2")

      val res = fstreeRepo.tree(Option(fromPath)).futureValue

      val resA = res.take(14)
      val resC = res.slice(14, 25)
      val resD = res.slice(25, 34)

      forAll(resA.zip(2 to 15)) { r =>
        r._1.filename mustBe s"fstreefolderA_${r._2}"
      }

      forAll(resC.zip(1 to 11)) { r =>
        r._1.filename mustBe s"fstreefolderC_${r._2}"
      }

      forAll(resD.zip(1 to 9)) { r =>
        r._1.filename mustBe s"fstreefolderD_${r._2}"
      }
    }

    "return all direct children for given path" in {
      val fromPath =
        Path.root.append("fstreefolderA_1").append("fstreefolderA_2")

      val res = fstreeRepo.children(Option(fromPath)).futureValue

      res.size mustBe 3
    }
  }

  // scalastyle:on magic.number
}
