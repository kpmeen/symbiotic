package util.specs

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import net.scalytica.symbiotic.config.ConfigReader
import net.scalytica.symbiotic.test.specs.MongoSpec
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

trait ServerMongoSpec extends PlaySpec with GuiceOneAppPerSuite with MongoSpec {

  val defaultDBName = "test_symbiotic"

  val cfgFile = "test-mongo.conf"

  sys.props += "config.resource" -> cfgFile

  override val configuration = Configuration(ConfigReader.load())

  implicit val actorSys: ActorSystem           = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(configuration).build()

  override def clean(): Either[String, Unit] = {
    super.clean()
    if (!preserveDB) {
      MongoClient(MongoClientURI(localTestDBURI))(defaultDBName).dropDatabase()
      Right(())
    } else {
      Left(
        s"Preserving $defaultDBName DB as requested." +
          s" ¡¡¡IMPORTANT!!! DROP DB BEFORE NEW TEST RUN!"
      )
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()

    materializer.shutdown()
    actorSys.terminate()
  }

}
