package net.scalytica.symbiotic.data

import net.scalytica.symbiotic.data.PartyBaseTypes.UserId

case class ManagedFileMetadata(
    owner: Option[UserId] = None,
    fid: Option[FileId] = None,
    uploadedBy: Option[UserId] = None,
    version: Version = 1,
    isFolder: Option[Boolean] = None,
    path: Option[Path] = None,
    description: Option[String] = None,
    lock: Option[Lock] = None
) extends BaseMetadata
