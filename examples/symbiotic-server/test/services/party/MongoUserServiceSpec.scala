package services.party

import util.specs.ServerMongoSpec

class MongoUserServiceSpec extends ServerMongoSpec with UserServiceSpec {

  override val service = app.injector.instanceOf[UserService]

}
