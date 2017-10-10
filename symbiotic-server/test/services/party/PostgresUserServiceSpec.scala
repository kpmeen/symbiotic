package services.party

import repository.postgres.party.PostgresUserRepository
import util.testdata.ServerPostgresSpec

class PostgresUserServiceSpec extends ServerPostgresSpec with UserServiceSpec {

  override lazy val repo = app.injector.instanceOf[PostgresUserRepository]

}
