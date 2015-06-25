/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package net.scalytica.symbiotic.css

object FileTypes {

  sealed trait FileTypes

  case object Folder extends FileTypes

  case object GenericFile extends FileTypes

}
