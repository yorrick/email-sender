import controllers.{JsonFormats, Sms}

import java.util.logging

import com.github.athieriot.{EmbedConnection, CleanAfterExample}
import de.flapdoodle.embed.mongo.{Command, MongodStarter}
import de.flapdoodle.embed.mongo.config.{RuntimeConfigBuilder, MongodConfigBuilder}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.io.ProcessOutput
import org.junit.runner.RunWith
import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import org.specs2.runner._
import org.specs2.specification.{Scope, AfterExample}

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.{Format, Json}
import play.api.{Application, Logger}
import play.api.test._
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.LastError

import scala.concurrent.{Future, Await}


/**
 * Remove all logs from embed mongodb
 */
trait QuietEmbedConnection extends EmbedConnection {
  self: Specification =>

  override def embedMongoDBVersion(): Version.Main = { Version.Main.V2_6 }

  val logger = logging.Logger.getLogger(getClass().getName());

  // removes all logging from embedmongo
  lazy val  runtimeConfig = new RuntimeConfigBuilder()
    .defaultsWithLogger(Command.MongoD, logger)
    .processOutput(ProcessOutput.getDefaultInstanceSilent())
    .build();
  override lazy val runtime =  MongodStarter.getInstance(runtimeConfig)
}

//
///**
// * Provides a connection and a collection to a test
// */
//trait InitDB { self: WithApplication =>
//
//  val collectionName: String
//  def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
//  def collection: JSONCollection = db.collection[JSONCollection](collectionName)
//
////  override def around[T: AsResult](t: => T): Result = {
////    val newBlock = { () =>
////      println("Executing before")
////      before
////      println("Executed before")
////      t
////    }
////    Helpers.running(app)(AsResult.effectively(newBlock))
////  }
////
////  def before: Any
//
//}

trait DBInitializer {
  def setup: Application => Unit = _ => Unit
  def teardown: Application => Unit = _ => Unit
}


abstract class WithEnv(val app: FakeApplication = FakeApplication(),
                       var setup: Application => Unit = _ => Unit,
                       teardown: Application => Unit = _ => Unit) extends Around with Scope {

  implicit def implicitApp = app

  override def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively {
      setup(app)
      try {
        t
      } finally {
        teardown(app)
      }
    })
  }
}


class InitDB[T](val data: Tuple3[String, List[T], Format[T]]*) extends Around with Scope {

  import scala.concurrent.duration._
  import play.api.libs.concurrent.Execution.Implicits._

  lazy val app = FakeApplication()

  override def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively {
      initDB(app)
      try {
        t
      } finally {
//        teardown(app)
      }
    })
  }

  def initDB(app: Application) {
    implicit val application = app
    val timeout = 20.seconds
    val db: reactivemongo.api.DB = ReactiveMongoPlugin.db

    data.foreach {
      case (collectionName, initialObjects, format) =>
        Logger.warn(s"Initializing collection $collectionName with data $initialObjects")
        val collection: JSONCollection = db.collection[JSONCollection](collectionName)

        // recover in case the collection does not exist
        val dropFuture = collection.drop() recover {
          case t @ _ =>
            false
        }
        Await.result(dropFuture, timeout)

        Logger.warn(s"Inserting data in DB")
        implicit val implicitFormat = format
//        val bsonObjects = initialObjects.map(obj => format.writes(obj))
        val bsonObjects: List[BSONDocument] = initialObjects.map(obj => format.writes(obj).asInstanceOf[BSONDocument])

        val insertFuture: Future[Int] = collection.bulkInsert(Enumerator.enumerate(bsonObjects)).mapTo[Int] recover {
          case t @ _ =>
            Logger.warn(s"Recovered $t")
            0
        }
        Logger.warn(s"Inserting data in DB DONE")
        val inserted = Await.result(insertFuture, timeout)

        Logger.warn(s"Inserting data in DB DONE")
        if (inserted < initialObjects.length) {
          throw new Exception(s"Could not insert all initial data of collection $initialObjects: inserted $inserted instead of ${initialObjects.length}")
        }
    }

  }
}



@RunWith(classOf[JUnitRunner])
class SmsSpec extends Specification with PlaySpecification {
  sequential
//  isolated

  step {
    Logger.info("Before class")
  }

  val smsData = ("smsList", List(Sms("11111111", "222222222", "some text")), JsonFormats.smsFormat)

//  def setup(app: Application) {
//    Logger.info("------------------------------------------INIT DATABASE")
//    implicit val application = app
//
//    val db: reactivemongo.api.DB = ReactiveMongoPlugin.db
//    val collection: JSONCollection = db.collection[JSONCollection]("smslist")
//
//    val dropFuture = collection.drop()
//    await(dropFuture)
//
//    import JsonFormats._
//    val sms = Sms("11111111", "222222222", "some text")
//
//    val insertFuture = collection.insert(sms).mapTo[LastError] map { lastError =>
//      if (lastError.inError == true) {
//        val message = s"Could not save the sms: ${lastError.message}"
//        Logger.warn(message)
//      } else {
//        Logger.warn(s"Ok: $lastError")
//      }
//    }
//
//    await(insertFuture)
//  }

  "Sms module" should {

//    "render the sms list page" in new WithEnv(setup = setup) {
    "render the sms list page" in new InitDB(data=smsData) {

      Logger.info("------------------------------------------Start test XXXXXXXXXXXXXXX")
      val response = controllers.SmsService.list()(FakeRequest())

      status(response) must equalTo(OK)
      contentType(response) must beSome.which(_ == "text/html")
      contentAsString(response) must contain ("List")

      Logger.info(s"------------------------------------------${contentAsString(response)}")
    }

//    "Accept post data for sms" in new WithApplication with SmsInitDB {
//      Logger.info("------------------------------------------Start test XXXXXXXXXXXXXXX")
//
//      val request = FakeRequest(POST, "/sms/").withFormUrlEncodedBody(
//        "To" -> "666666666",
//        "From" -> "77777777",
//        "Body" -> "hello toto"
//      )
//
//      val response = controllers.SmsService.receive()(request)
//
//      status(response) must equalTo(OK)
//      contentAsString(response) must contain("Message")
//
//      import JsonFormats._
//      val cursor: Cursor[Sms] = collection.
//        // find all sms
//        find(Json.obj()).
//        // perform the query and get a cursor of JsObject
//        cursor[Sms]
//      val future = cursor.collect[List]()
//      val result = await(future)
//
//      result should have size (1)
//    }

  }

  "Main module" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = controllers.Application.index()(FakeRequest())

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain("Send emails")
    }
  }

  step {
    Logger.info("After class")
  }
}
