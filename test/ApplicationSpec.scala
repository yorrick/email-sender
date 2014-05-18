import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.Files._
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication {
      route(FakeRequest(GET, "/boum")) must beNone
    }

    "render the index page" in new WithApplication {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Hello Play Framework")
    }

    "Accept post data for sms" in new WithApplication {

      val request = FakeRequest(POST, "/sms/").withFormUrlEncodedBody(
        "To" -> "666666666",
        "From" -> "77777777",
        "Body" -> "hello toto"
      )

      val response = route(request).get

      status(response) must equalTo(OK)
      contentAsString(response) must contain ("Message")

      /*val data = new MultipartFormData(
        Map(
          ("param1" -> Seq("test-1")), 
          ("param2" -> Seq("test-2"))
        ), 
        List(
          FilePart("payload", "message", Some("Content-Type: multipart/form-data"), 
            play.api.libs.Files.TemporaryFile(new java.io.File("/tmp/pepe.txt")))
        ), 
        List(), 
        List()
      )

      val Some(result) = routeAndCall(FakeRequest(POST, "/sms/", FakeHeaders(), data))
      */

      //withFormUrlEncodedBody
      //val sms = route(FakeRequest(POST, "/sms/")).post
    }

  }


}
