package ems.utils

import play.api.test.FakeApplication


/**
 * Contains mocks that are used in the tests
 */
trait AppUtils {

  val mongoPluginClass = "play.modules.reactivemongo.ReactiveMongoPlugin"
  val redisPluginClass = "play.modules.rediscala.RedisPlugin"
  def noRedisApp = FakeApplication(withoutPlugins = Seq(redisPluginClass))
  def noMongoApp = FakeApplication(withoutPlugins = Seq(mongoPluginClass))
  def app = FakeApplication(withoutPlugins = Seq(mongoPluginClass, redisPluginClass))

}
