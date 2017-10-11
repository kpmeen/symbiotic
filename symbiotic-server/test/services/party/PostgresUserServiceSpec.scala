package services.party

import net.scalytica.symbiotic.core.DocManagementService
import repository.postgres.party.PostgresUserRepository
import util.testdata.ServerPostgresSpec

class PostgresUserServiceSpec extends ServerPostgresSpec with UserServiceSpec {

  override lazy val repo       = app.injector.instanceOf[PostgresUserRepository]
  override lazy val docService = app.injector.instanceOf[DocManagementService]

}
