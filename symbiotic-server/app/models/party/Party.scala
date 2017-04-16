package models.party

import net.scalytica.symbiotic.api.types.PersistentType
import net.scalytica.symbiotic.data.PartyBaseTypes.PartyId

/**
 * An abstraction defining a Party (person or organsation)
 */
abstract class Party extends PersistentType {
  val id: Option[PartyId]
}
