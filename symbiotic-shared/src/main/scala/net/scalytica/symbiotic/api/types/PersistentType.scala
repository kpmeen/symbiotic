package net.scalytica.symbiotic.api.types

import net.scalytica.symbiotic.api.types.PartyBaseTypes.UserId
import net.scalytica.symbiotic.api.types.PersistentType.VersionStamp
import net.scalytica.symbiotic.time.SymbioticDateTime._
import org.joda.time.DateTime

trait PersistentType {
  val v: Option[VersionStamp]
}

object PersistentType {

  case class UserStamp(date: DateTime, by: UserId)

  object UserStamp {
    def create(uid: UserId): UserStamp = UserStamp(now, uid)
  }

  case class VersionStamp(
      version: Int = 1,
      created: Option[UserStamp] = None,
      modified: Option[UserStamp] = None
  )

}
