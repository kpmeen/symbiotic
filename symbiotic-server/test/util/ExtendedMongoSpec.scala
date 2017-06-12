package util

import com.mongodb.casbah.{MongoClient, MongoClientURI}
import net.scalytica.symbiotic.test.specs.MongoSpec
import play.api.Configuration

trait ExtendedMongoSpec extends MongoSpec {

  val testDBName = "test_symbiotic"

  val extConfiguration = configuration ++ Configuration(
    "symbiotic.mongodb.dbname.default" -> testDBName
  )

  override val config = extConfiguration.underlying

  override def initDatabase() = {
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(testDBName).dropDatabase()
    } else {
      println(s"[WARN] Preserving $testDBName DB as requested.")
    }

    super.initDatabase()
  }
}
