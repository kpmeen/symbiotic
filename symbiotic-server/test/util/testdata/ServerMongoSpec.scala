package util.testdata

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.casbah.{MongoClient, MongoClientURI}
import com.typesafe.config.ConfigFactory
import net.scalytica.symbiotic.test.specs.MongoSpec
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}

trait ServerMongoSpec extends PlaySpec with GuiceOneAppPerSuite with MongoSpec {

  val defaultDBName = "test_symbiotic"

  override val configuration = Configuration(
    ConfigFactory.load("test-mongo.conf").resolve()
  )

  implicit val actorSys: ActorSystem           = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(configuration).build()

  override implicit lazy val app: Application = fakeApplication()

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

}
