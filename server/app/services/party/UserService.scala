/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package services.party

import com.google.inject.{Inject, Singleton}
import core.lib.SuccessOrFailure
import models.base.Username
import models.party.PartyBaseTypes.UserId
import models.party.User
import repository.UserRepository

@Singleton
class UserService @Inject() (val userRepository: UserRepository) {

  def save(user: User): SuccessOrFailure = userRepository.save(user)

  def findById(id: UserId): Option[User] = userRepository.findById(id)

  def findByUsername(username: Username): Option[User] = userRepository.findByUsername(username)

}
