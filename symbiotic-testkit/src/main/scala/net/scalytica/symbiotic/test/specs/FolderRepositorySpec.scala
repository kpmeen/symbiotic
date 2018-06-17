package net.scalytica.symbiotic.test.specs

import net.scalytica.symbiotic.api.SymbioticResults.{NotFound, NotModified, Ok}
import net.scalytica.symbiotic.api.repository.FolderRepository
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.ResourceParties.{
  AllowedParty,
  Org,
  Owner
}
import net.scalytica.symbiotic.api.types.{Folder, FolderId, Path}
import net.scalytica.symbiotic.test.generators.{
  FolderGenerator,
  TestContext,
  TestOrgId,
  TestUserId
}
import net.scalytica.symbiotic.test.utils.SymResValues
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, OptionValues, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class FolderRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with OptionValues
    with SymResValues
    with PersistenceSpec {

  // scalastyle:off magic.number
  val usrId1 = TestUserId.create()
  val orgId1 = TestOrgId.create()
  val owner  = Owner(orgId1, Org)

  val usrId2 = TestUserId.create()
  val orgId2 = TestOrgId.create()

  val accessors = Seq(AllowedParty(usrId2), AllowedParty(orgId2))

  implicit val ctx: TestContext = TestContext(usrId1, owner, Seq(owner.id))
  val ctx2                      = TestContext(usrId2, owner, Seq(usrId2))

  val folderRepo: FolderRepository

  val folders = {
    Seq(Folder(orgId1, Path.root)) ++
      FolderGenerator.createFolders(
        owner = orgId1,
        baseName = "fstreefolderA",
        depth = 15
      ) ++ FolderGenerator.createFolders(
      owner = orgId1,
      baseName = "fstreefolderB",
      depth = 5,
      accessibleBy = accessors
    ) ++ FolderGenerator.createFolders(
      owner = orgId1,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderC",
      depth = 11
    ) ++ FolderGenerator.createFolders(
      owner = orgId1,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderD",
      depth = 9
    )
  }

  "The folder repository" should {

    val folderIds = Seq.newBuilder[FolderId]

    "successfully save some folders" in {
      val res =
        Future
          .sequence(folders.map(f => folderRepo.save(f)))
          .futureValue
          .flatMap(_.toOption)

      res.size mustBe 41

      folderIds ++= res
    }

    "successfully save a folder with some extra metadata attributes" in {
      val parent = folders(18)
      val ea = Option(
        MetadataMap(
          "foo"  -> "bar",
          "fizz" -> false,
          "buzz" -> 10.01d
        )
      )
      val f = FolderGenerator.createFolder(
        owner = orgId1,
        from = parent.flattenPath,
        name = "testfolder_extraAttribs",
        folderType = Some("custom folder"),
        extraAttributes = ea
      )

      val res = folderRepo.save(f).futureValue
      res.foreach(fid => folderIds += fid)

      res.success mustBe true
    }

    "return a folder with a specific id" in {
      val fid      = folderIds.result()(6)
      val expected = folders(6)

      val res = folderRepo.get(fid).futureValue.value

      res.metadata.fid mustBe Some(fid)
      res.filename mustBe expected.filename
      res.flattenPath.materialize mustBe expected.flattenPath.materialize
    }

    "return a folder when user has access but isn't the owner" in {
      val fid      = folderIds.result()(17)
      val expected = folders(17)
      val res      = folderRepo.get(fid)(ctx2, global).futureValue.value

      res.metadata.fid mustBe Some(fid)
      res.filename mustBe expected.filename
      res.flattenPath.materialize mustBe expected.flattenPath.materialize
    }

    "not return a folder when user doesn't have access" in {
      val fid = folderIds.result()(6)
      folderRepo.get(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "return a folder with a specific id that contains extra metadata" in {
      val fid = folderIds.result().lastOption.value
      val res = folderRepo.get(fid).futureValue.value

      res.metadata.fid mustBe Some(fid)
      res.filename mustBe "testfolder_extraAttribs"
      res.flattenPath.parent mustBe folders(18).flattenPath
      res.fileType mustBe Some("custom folder")
      res.metadata.extraAttributes must not be empty

      val ea = res.metadata.extraAttributes.value
      ea.get("foo") mustBe Some(StrValue("bar"))
      ea.get("fizz") mustBe Some(BoolValue(false))
      ea.get("buzz") mustBe Some(DoubleValue(10.01d))
    }

    "return true if a folder path exists" in {
      val expected = folders(5).flattenPath
      folderRepo.exists(expected).futureValue mustBe true
    }

    "return false if a folder path exists but user doesn't have access" in {
      val expected = folders(5).flattenPath
      folderRepo.exists(expected)(ctx2, global).futureValue mustBe false
    }

    "return true if a folder exists" in {
      val expected = folders(5)
      folderRepo.exists(expected).futureValue mustBe true
    }

    "return false if a folder exists but user doesn't have access" in {
      val expected = folders(5)
      folderRepo.exists(expected)(ctx2, global).futureValue mustBe false
    }

    "return false if a folder path doesn't exist" in {
      folderRepo.exists(Path("/foo/bar/baz")).futureValue mustBe false
    }

    "return a list of paths that doesn't exist in a given path" in {
      val exp1 =
        folders(6).flattenPath.append("foo").append("bar").append("baz")
      val exp2 = exp1.parent
      val exp3 = exp2.parent

      val res = folderRepo.filterMissing(exp1).futureValue.value

      res.size mustBe 3
      res must contain allOf (exp1, exp2, exp3)
    }

    "move a folder and all its children from one path to another" in {
      val fid  = folderIds.result()(7)
      val orig = folders(7)
      val to   = folders(20).flattenPath.append(orig.filename)

      folderRepo.move(orig.flattenPath, to).futureValue mustBe Ok(9)

      // validate the move occurred
      val res1 = folderRepo.get(fid).futureValue.value
      res1.flattenPath mustBe to
      res1.filename mustBe orig.filename
    }

    "not be allowed to move a folder without access" in {
      val orig = folders(7)
      val to   = folders(20).flattenPath.append(orig.filename)

      folderRepo
        .move(orig.flattenPath, to)(ctx2, global)
        .futureValue mustBe NotModified()
    }

    "not lock a folder when user doesn't have access" in {
      val fid = folderIds.result()(7)

      folderRepo.lock(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "lock a folder" in {
      val fid = folderIds.result()(7)
      folderRepo.lock(fid).futureValue match {
        case Ok(lock) =>
          lock.by mustBe usrId1
          lock.date.getDayOfYear mustBe DateTime.now.getDayOfYear

        case err =>
          fail(s"Expected LockApplied[Option[Lock]], got ${err.getClass}")
      }
    }

    "return the user id of the lock owner on a locked folder" in {
      val fid = folderIds.result()(7)
      folderRepo.locked(fid).futureValue.value mustBe Some(usrId1)
    }

    "not unlock a folder when user doesn't have access" in {
      val fid = folderIds.result()(7)

      folderRepo.unlock(fid)(ctx2, global).futureValue mustBe NotFound()
    }

    "unlock a folder" in {
      val fid = folderIds.result()(7)
      folderRepo.unlock(fid).futureValue mustBe Ok(())
    }

    "return None if the folder isn't locked" in {
      val fid = folderIds.result()(7)
      folderRepo.locked(fid).futureValue.value mustBe None
    }

    "successfully mark a folder as deleted" in {
      val fid = folderIds.result()(7)

      folderRepo.markAsDeleted(fid).futureValue mustBe Ok(1)
    }

    "not return information about a deleted folder" in {
      val fid = folderIds.result()(7)

      folderRepo.findLatestBy(fid).futureValue mustBe NotFound()
    }

  }

}
