package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.{Folder, Path}

object FolderGenerator {

  def createFolders(
      owner: PartyId,
      from: Path = Path.root,
      baseName: String = "testfolder",
      depth: Int = 10
  ): Seq[Folder] = {
    (1 to depth).foldLeft(Seq.empty[Folder]) { (folders, curr) =>
      val p = folders.lastOption.map(_.flattenPath).getOrElse(from)
      folders :+ createFolder(owner, p, s"${baseName}_$curr")
    }
  }

  def createFolder(
      owner: PartyId,
      from: Path = Path.root,
      name: String = "testfolder",
      folderType: Option[String] = None,
      extraAttributes: Option[MetadataMap] = None
  ): Folder = {
    Folder(
      owner = owner,
      path = from.append(name),
      tpe = folderType,
      extraAttributes = extraAttributes
    )
  }

}
