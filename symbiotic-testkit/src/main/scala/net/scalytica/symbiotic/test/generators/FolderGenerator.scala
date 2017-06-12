package net.scalytica.symbiotic.test.generators

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.{Folder, Path}

object FolderGenerator {

  def createFolders(
      owner: UserId,
      from: Path = Path.root,
      baseName: String = "testfolder",
      depth: Int = 10
  ): Seq[Folder] = {
    (1 to depth).foldLeft(Seq.empty[Folder]) { (folders, curr) =>
      val p = folders.lastOption.map(_.flattenPath).getOrElse(from)
      folders :+ Folder(owner, p.append(s"${baseName}_$curr"))
    }
  }

}
