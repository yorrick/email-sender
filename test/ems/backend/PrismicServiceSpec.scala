package ems.backend

import ems.backend.cms.PrismicService
import ems.utils.{AppInjector, TestUtils, WithTestData}
import org.junit.runner.RunWith
import org.specs2.runner._
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, Handler}
import play.api.test._
import scaldi.Injectable


@RunWith(classOf[JUnitRunner])
class PrismicServiceSpec extends PlaySpecification with WithTestData with AppInjector with Injectable with TestUtils {
  sequential

  val routes: PartialFunction[(String, String), Handler] = {
    case ("GET", _: String) =>
      Action { Ok("TODO") }
  }

  override val app = FakeApplication(withRoutes = routes, withoutPlugins = Seq(mongoPluginClass, redisPluginClass))

  "Prismic service" should {

    "Fetch main page doc" in new WithServer(app = app) {
      // TODO use fake url
      implicit val injector = appInjector
      val service = inject[PrismicService]

      await(service.getMainPageDocument) must beSome
    }

  }
}
