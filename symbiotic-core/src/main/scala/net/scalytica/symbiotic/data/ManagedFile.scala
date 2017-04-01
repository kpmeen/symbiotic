/**
 * Copyright(c) 2017 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.data

trait ManagedFile extends GridFSDocument[ManagedFileMetadata]

trait ManagedFileExtensions[A <: ManagedFile] {

  /**
   * Maps the ManagedFile arg to the type of A, which must be a sub-class of
   * ManagedFile.
   *
   * @param mf ManagedFile to map
   * @return An Option containing the ManagedFile as an instance of A.
   */
  def mapTo(mf: ManagedFile): Option[A]

}
