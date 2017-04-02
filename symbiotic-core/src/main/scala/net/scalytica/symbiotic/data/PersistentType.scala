package net.scalytica.symbiotic.data

import net.scalytica.symbiotic.data.PartyBaseTypes.UserId
import net.scalytica.symbiotic.data.PersistentType.VersionStamp
import org.joda.time.DateTime

trait PersistentType {
  val v: Option[VersionStamp]
}

object PersistentType {

  case class UserStamp(date: DateTime, by: UserId)

  object UserStamp {
    def create(uid: UserId): UserStamp = UserStamp(DateTime.now, uid)
  }

  case class VersionStamp(
      version: Int = 1,
      created: Option[UserStamp] = None,
      modified: Option[UserStamp] = None
  )

  object VersionStamp {}

}
