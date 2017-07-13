package net.scalytica.symbiotic.test.specs

import net.scalytica.symbiotic.api.persistence.FolderRepository
import net.scalytica.symbiotic.api.types.CommandStatusTypes.CommandOk
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.Implicits._
import net.scalytica.symbiotic.api.types.CustomMetadataAttributes._
import net.scalytica.symbiotic.api.types.{Folder, FolderId, Path}
import net.scalytica.symbiotic.test.generators.{FolderGenerator, TestUserId}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class FolderRepositorySpec
    extends WordSpecLike
    with ScalaFutures
    with MustMatchers
    with BeforeAndAfterAll {

  // scalastyle:off magic.number
  implicit val uid       = TestUserId.create()
  implicit val transform = (s: String) => TestUserId.asId(s)

  val folderRepo: FolderRepository

  val folders = {
    Seq(Folder(uid, Path.root)) ++
      FolderGenerator.createFolders(
        owner = uid,
        baseName = "fstreefolderA",
        depth = 15
      ) ++ FolderGenerator.createFolders(
      owner = uid,
      baseName = "fstreefolderB",
      depth = 5
    ) ++ FolderGenerator.createFolders(
      owner = uid,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderC",
      depth = 11
    ) ++ FolderGenerator.createFolders(
      owner = uid,
      from = Path.root.append("fstreefolderA_1").append("fstreefolderA_2"),
      baseName = "fstreefolderD",
      depth = 9
    )
  }

  "The folder repository" should {

    val folderIds = Seq.newBuilder[FolderId]

    "successfully save some folders" in {
      val res =
        Future.sequence(folders.map(f => folderRepo.save(f))).futureValue
      res.size mustBe 41

      folderIds ++= res.flatten
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
        owner = uid,
        from = parent.flattenPath,
        name = "testfolder_extraAttribs",
        folderType = Some("custom folder"),
        extraAttributes = ea
      )

      val res = folderRepo.save(f).futureValue
      res.foreach(fid => folderIds += fid)

      res must not be empty
    }

    "return a folder with a specific id" in {
      val fid      = folderIds.result()(6)
      val expected = folders(6)
      val res      = folderRepo.get(fid).futureValue

      res must not be empty
      val folder = res.get

      folder.metadata.fid mustBe Some(fid)
      folder.filename mustBe expected.filename
      folder.flattenPath.materialize mustBe expected.flattenPath.materialize
    }

    "return a folder with a specific id that contains extra metadata" in {
      val fid = folderIds.result().last
      val res = folderRepo.get(fid).futureValue

      res must not be empty
      val folder = res.get

      folder.metadata.fid mustBe Some(fid)
      folder.filename mustBe "testfolder_extraAttribs"
      folder.flattenPath.parent mustBe folders(18).flattenPath
      folder.fileType mustBe Some("custom folder")
      folder.metadata.extraAttributes must not be empty

      val ea = folder.metadata.extraAttributes.get
      ea.get("foo") mustBe Some(StrValue("bar"))
      ea.get("fizz") mustBe Some(BoolValue(false))
      ea.get("buzz") mustBe Some(DoubleValue(10.01d))
    }

    "return true if a folder path exists" in {
      val expected = folders(5).flattenPath
      folderRepo.exists(expected).futureValue mustBe true
    }

    "return true if a folder exists" in {
      val expected = folders(5)
      folderRepo.exists(expected).futureValue mustBe true
    }

    "return false if a folder path doesn't exist" in {
      folderRepo.exists(Path("/foo/bar/baz")).futureValue mustBe false
    }

    "return a list of paths that doesn't exist in a given path" in {
      val exp1 =
        folders(6).flattenPath.append("foo").append("bar").append("baz")
      val exp2 = exp1.parent
      val exp3 = exp2.parent

      val res = folderRepo.filterMissing(exp1).futureValue

      res.size mustBe 3
      res must contain allOf (exp1, exp2, exp3)
    }

    "move a folder from one path to another" in {
      val fid        = folderIds.result()(7)
      val orig       = folders(7)
      val firstChild = folders(8)
      val to         = folders(20).flattenPath.append(orig.filename)

      folderRepo.move(orig.flattenPath, to).futureValue mustBe CommandOk(1)

      // validate the move occurred
      val res1 = folderRepo.get(fid).futureValue
      res1 must not be empty
      res1.get.flattenPath mustBe to
      res1.get.filename mustBe orig.filename
    }

  }

}
