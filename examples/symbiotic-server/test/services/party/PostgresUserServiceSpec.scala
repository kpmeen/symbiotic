package services.party

import util.specs.ServerPostgresSpec

class PostgresUserServiceSpec extends ServerPostgresSpec with UserServiceSpec {

  override val service = app.injector.instanceOf[UserService]

}
