package models.party

import net.scalytica.symbiotic.api.types.PartyBaseTypes.PartyId
import net.scalytica.symbiotic.api.types.PersistentType

/**
 * An abstraction defining a Party (person or organisation)
 */
abstract class Party extends PersistentType {
  val id: Option[PartyId]
}
