package services.party

import repository.mongodb.party.MongoDBUserRepository
import util.testdata.ServerMongoSpec

class MongoUserServiceSpec extends ServerMongoSpec with UserServiceSpec {

  override lazy val repo = app.injector.instanceOf[MongoDBUserRepository]

}
