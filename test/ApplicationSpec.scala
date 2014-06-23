import java.util.concurrent.Future
import java.util.logging.Logger

import com.github.athieriot.{EmbedConnection, CleanAfterExample}
import controllers.{JsonFormats, Sms}
import de.flapdoodle.embed.mongo.{Command, MongodStarter}
import de.flapdoodle.embed.mongo.config.{RuntimeConfigBuilder, MongodConfigBuilder}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.process.config.io.ProcessOutput
import org.specs2.execute.{Result, AsResult}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.specification._
import org.specs2.specification
import org.specs2.mutable

import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.Cursor


/**
 * Provides a connection and a collection to a test
 */
trait InitDB {
  self: WithApplication =>

    val collectionName: String
    def db: reactivemongo.api.DB = ReactiveMongoPlugin.db
    def collection: JSONCollection = db.collection[JSONCollection](collectionName)

    override def around[T: AsResult](t: => T): Result = {
      before
      Helpers.running(app)(AsResult.effectively(t))
    }

    def before: Any

}

/**
 * Remove all logs from embed mongodb
 */
trait QuietEmbedConnection extends EmbedConnection {
  self: Specification =>

  override def embedMongoDBVersion(): Version.Main = { Version.Main.V2_6 }

  val logger = Logger.getLogger(getClass().getName());

  // removes all logging from embedmongo
  lazy val  runtimeConfig = new RuntimeConfigBuilder()
    .defaultsWithLogger(Command.MongoD, logger)
    .processOutput(ProcessOutput.getDefaultInstanceSilent())
    .build();
  override lazy val runtime =  MongodStarter.getInstance(runtimeConfig)
}


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class SmsSpec extends Specification with QuietEmbedConnection with PlaySpecification {
  sequential
//  isolated

  step {
    println("Before class")
  }

  trait SmsInitDB extends InitDB { self: WithApplication =>
    val collectionName = "smsList"
    def before = println("Should initialize DB here")
  }

  "Sms module" should {

    "Accept post data for sms" in new WithApplication with SmsInitDB {

      val request = FakeRequest(POST, "/sms/").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val response = controllers.SmsService.receive()(request)

      status(response) must equalTo(OK)
      contentAsString(response) must contain("Message")

      import JsonFormats._
      val cursor: Cursor[Sms] = collection.
        // find all sms
        find(Json.obj()).
        // perform the query and get a cursor of JsObject
        cursor[Sms]
//      val future : Future[List[Sms]] = cursor.collect[List]()
      val future = cursor.collect[List]()
      val result = await(future)

      println(db)
      println(s"TOTO: $result")
    }

  }

  "The 'Hello world' string" should {
    "contain 11 characters" in {
      running(FakeApplication()) {
        println("contain 11 characters")
        "Hello world" must have size (11)
      }
    }
  }
//    "start with 'Hello'" in {
//      println("start with hello")
//      "Hello world" must startWith("Hello")
//    }
//  }

  step {
    println("After class")
  }
}


/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
//class ApplicationSpec extends Specification with BeforeAfter { sequential
class ApplicationSpec extends Specification { sequential

  "Application" should {

//    "send 404 on a bad request" in new WithApplication {
//      route(FakeRequest(GET, "/boum")) must beNone
//    }
//
//    "render the index page" in new WithApplication {
//      val home = controllers.Application.index()(FakeRequest())
//
//      status(home) must equalTo(OK)
//      contentType(home) must beSome.which(_ == "text/html")
//      contentAsString(home) must contain ("Send emails")
//    }

    "Accept post data for sms" in new WithApplication {

      val request = FakeRequest(POST, "/sms/").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val response = controllers.SmsService.receive()(request)

      status(response) must equalTo(OK)
      contentAsString(response) must contain ("Message")
    }

//    "render the sms list page" in new WithApplication {
//      val response = controllers.SmsService.list()(FakeRequest())
//
//      status(response) must equalTo(OK)
//      contentType(response) must beSome.which(_ == "text/html")
//      contentAsString(response) must contain ("List")
//    }


  }


}
