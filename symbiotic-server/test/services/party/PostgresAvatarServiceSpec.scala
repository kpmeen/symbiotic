package services.party

import repository.postgres.party.PostgresAvatarRepository
import util.testdata.ServerPostgresSpec

class PostgresAvatarServiceSpec
    extends ServerPostgresSpec
    with AvatarServiceSpec {

  override lazy val repo = app.injector.instanceOf[PostgresAvatarRepository]

}
