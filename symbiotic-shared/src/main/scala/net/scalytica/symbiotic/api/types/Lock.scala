package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import org.joda.time.DateTime

/**
 * Used for handling (un)locking files for change (or version incrementation)
 */
case class Lock(by: UserId, date: DateTime)
