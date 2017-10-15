package services.party

import util.specs.ServerMongoSpec

class MongoAvatarServiceSpec extends ServerMongoSpec with AvatarServiceSpec {

  override val service = app.injector.instanceOf[AvatarService]

}
