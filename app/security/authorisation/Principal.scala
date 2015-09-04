/**
 * Copyright(c) 2015 Knut Petter Meen, all rights reserved.
 */
package security.authorisation

import play.api.libs.json.Format

trait Principal {

}

object Principal {

  implicit val format: Format[Principal] = ???

}
