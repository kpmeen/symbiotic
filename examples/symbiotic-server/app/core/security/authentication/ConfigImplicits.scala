package core.security.authentication

import com.typesafe.config.Config
import net.ceedubs.ficus.readers.ValueReader
import play.api.mvc.Cookie

trait ConfigImplicits {

  /**
   * A very nested optional reader, to support these cases:
   * Not set, set None, will use default ('Lax')
   * Set to null, set Some(None), will use 'No Restriction'
   * Set to a string value try to match, Some(Option(string))
   */
  implicit val sameSiteReader: ValueReader[Option[Option[Cookie.SameSite]]] =
    (config: Config, path: String) => {
      if (config.hasPathOrNull(path)) {
        if (config.getIsNull(path)) Some(None)
        else Some(Cookie.SameSite.parse(config.getString(path)))
      } else {
        None
      }
    }

}
