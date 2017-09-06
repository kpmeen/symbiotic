package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.CustomMetadataAttributes.MetadataMap
import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.ResourceParties.AllowedParty
import net.scalytica.symbiotic.api.types.{Folder, Path}

object FolderGenerator {

  // scalastyle:off magic.number

  def createFolders(
      owner: PartyId,
      from: Path = Path.root,
      baseName: String = "testfolder",
      depth: Int = 10,
      accessibleBy: Seq[AllowedParty] = Seq.empty
  ): Seq[Folder] = {
    (1 to depth).foldLeft(Seq.empty[Folder]) { (folders, curr) =>
      val p = folders.lastOption.map(_.flattenPath).getOrElse(from)
      folders :+ createFolder(
        owner,
        p,
        s"${baseName}_$curr",
        accessibleBy = accessibleBy
      )
    }
  }

  def createFolder(
      owner: PartyId,
      from: Path = Path.root,
      name: String = "testfolder",
      folderType: Option[String] = None,
      extraAttributes: Option[MetadataMap] = None,
      accessibleBy: Seq[AllowedParty] = Seq.empty
  ): Folder = {
    Folder(
      owner = owner,
      accessibleBy = accessibleBy,
      path = from.append(name),
      tpe = folderType,
      extraAttributes = extraAttributes
    )
  }

  // scalastyle:on magic.number

}
