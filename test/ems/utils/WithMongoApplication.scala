package ems.utils


import org.specs2.execute.{Result, AsResult}
import play.api.libs.json.JsValue
import play.api.test.{WithApplication, FakeApplication}


/**
 * Context for mongodb based tests with an application

 * @param data
 * @param app
 */
abstract class WithMongoApplication(
     val data: Seq[(String, List[JsValue])] = Seq(),
     override val app: FakeApplication = FakeApplication()) extends WithApplication(app) with WithMongoTestUtils {

  override def around[T: AsResult](t: => T): Result = super.around {
    initMongo
    t
  }

}
