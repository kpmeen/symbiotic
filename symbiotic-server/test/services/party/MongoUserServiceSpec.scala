package services.party

import net.scalytica.symbiotic.core.DocManagementService
import repository.mongodb.party.MongoDBUserRepository
import util.testdata.ServerMongoSpec

class MongoUserServiceSpec extends ServerMongoSpec with UserServiceSpec {

  override lazy val repo       = app.injector.instanceOf[MongoDBUserRepository]
  override lazy val docService = app.injector.instanceOf[DocManagementService]

}
