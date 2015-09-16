/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package core.security

import models.base.Id

package object authorisation {

  trait Principal {
    self: Id =>
  }

}
