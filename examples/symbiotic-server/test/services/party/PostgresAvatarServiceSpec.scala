package services.party

import util.specs.ServerPostgresSpec

class PostgresAvatarServiceSpec
    extends ServerPostgresSpec
    with AvatarServiceSpec {

  override val service = app.injector.instanceOf[AvatarService]

}
