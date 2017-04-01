package models.party

import net.scalytica.symbiotic.data.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.data.PersistentType

/**
 * An abstraction defining a Party (person or organsation)
 */
abstract class Party extends PersistentType {
  val id: Option[PartyId]
}
