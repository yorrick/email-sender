package ems.utils


import org.specs2.execute.{Result, AsResult}
import play.api.libs.json.JsValue
import play.api.test.{WithServer, FakeApplication}


/**
 * Context for mongodb based tests with a server
 * @param data
 * @param app
 */
abstract class WithMongoServer(
     val data: Seq[(String, List[JsValue])] = Seq(),
     override val app: FakeApplication = FakeApplication()) extends WithServer(app) with WithMongoTestUtils {

  override def around[T: AsResult](t: => T): Result = super.around {
    initMongo
    t
  }

}
