package services.party

import repository.mongodb.party.MongoDBAvatarRepository
import util.testdata.ServerMongoSpec

class MongoAvatarServiceSpec extends ServerMongoSpec with AvatarServiceSpec {

  override lazy val repo = app.injector.instanceOf[MongoDBAvatarRepository]

}
