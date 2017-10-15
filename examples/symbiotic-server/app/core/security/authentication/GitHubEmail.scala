package core.security.authentication

import play.api.libs.json.{Format, Json}

case class GitHubEmail(email: String, primary: Boolean, verified: Boolean)

object GitHubEmail {
  implicit val formats: Format[GitHubEmail] = Json.format[GitHubEmail]
}
