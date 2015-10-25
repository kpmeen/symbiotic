/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security.authentication

import models.base.Password
import org.mindrot.jbcrypt.BCrypt

object Crypto {

  /**
   * Encrypts the password string using BCrypt hashing and a generated salt.
   *
   * @param password the password to encrypt
   *
   * @return the hashed and salted password
   */
  def encryptPassword(password: String): Password = Password(BCrypt.hashpw(password, BCrypt.gensalt))

  def encryptPassword(password: Password): Password = encryptPassword(password.value)

  /**
   * Compare input password with the hashed stored password
   *
   * @param password the plain text password
   * @param hashed the stored, hashed password
   *
   * @return true if the comparison indicates a match, else false
   */
  def isValidPassword(password: String, hashed: Password): Boolean = BCrypt.checkpw(password, hashed.value)

}
