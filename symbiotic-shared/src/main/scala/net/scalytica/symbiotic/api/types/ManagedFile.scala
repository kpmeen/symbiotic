package net.scalytica.symbiotic.api.types

/**
 * A ManagedFile is any file _or_ folder that is handled by the core engine.
 * Meaning, any file or folder that could be persisted with versioning and
 * metadata.
 */
trait ManagedFile extends SymbioticDocument[ManagedMetadata]

trait ManagedFileOps[A <: ManagedFile] {

  /**
   * Maps the ManagedFile arg to the type of A, which must be a sub-class of
   * ManagedFile.
   *
   * @param mf ManagedFile to map
   * @return An Option containing the ManagedFile as an instance of A.
   */
  def mapTo(mf: ManagedFile): Option[A]

}
