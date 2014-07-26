import java.io.File
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import scala.collection.immutable.ListMap
import scala.sys.SystemProperties

import securesocial.core.authenticator.{HttpHeaderAuthenticatorBuilder, CookieAuthenticatorBuilder}
import securesocial.core.services.AuthenticatorService
import com.typesafe.config.ConfigFactory
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.GoogleProvider
import securesocial.controllers.ViewTemplates

import play.api._
import play.api.libs.concurrent.Akka
import akka.pattern.gracefulStop

import ems.backend.{Redis, RedisCookieAuthenticatorStore, MyEventListener, MongoDBUserService}
import ems.controllers.EMSViewTemplates
import ems.models.User
import ems.backend.utils.WithControllerUtils


object Global extends GlobalSettings with WithControllerUtils {

  val CONFIG_FILE = "config.file"

  /**
   * Overrides default configuration
   * @param loadedConfig
   * @param path
   * @param classloader
   * @param mode
   * @return
   */
  override def onLoadConfig(loadedConfig: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    // check if we have to override config file
    val systemProperties = new SystemProperties()

    val overrideConfig = systemProperties.get(CONFIG_FILE) match {
      case Some(configFile) =>
        // system property specific
        Logger.info(s"Using configuration file $configFile based on $CONFIG_FILE jvm property")
        Configuration(ConfigFactory.load(configFile))
      case None =>
        // mode specific
        val configFile = s"application.${mode.toString.toLowerCase}.conf"
        Logger.info(s"Using configuration file $configFile based on $mode mode")
        Configuration(ConfigFactory.load(configFile))
    }

    super.onLoadConfig(loadedConfig ++ overrideConfig, path, classloader, mode)
  }


  override def onStart(app: Application) {
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")

    // wait until redisconnection actor has shut down
//    Redis.redisClient.stop()
//    val stopped: Future[Boolean] = gracefulStop(Redis.redisClient.redisConnection, 5.second)
//    Await.result(stopped, 5.seconds)
  }

  /**
   * The runtime environment for this sample app.
   */
  class EMSRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
    override lazy val userService: MongoDBUserService = new MongoDBUserService()

    // use AuthenticationStore based on redis (distributed)
    override lazy val authenticatorService = new AuthenticatorService(
      new CookieAuthenticatorBuilder[User](new RedisCookieAuthenticatorStore(), idGenerator)
    )

    override lazy val eventListeners = List(new MyEventListener())

    // override authentication views
    override lazy val viewTemplates: ViewTemplates = new EMSViewTemplates(this)

    override lazy val providers = ListMap(
      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google)))
    )
  }

  object EMSRuntimeEnvironment {
    val instance = new EMSRuntimeEnvironment()
  }

  /**
   * An implementation that checks if the controller expects a RuntimeEnvironment and
   * passes the instance to it if required.
   *
   * This can be replaced by any DI framework to inject it differently.
   *
   * @param controllerClass
   * @tparam A
   * @return
   */
  override def getControllerInstance[A](controllerClass: Class[A]): A =
    getControllerInstance[A, User](EMSRuntimeEnvironment.instance)(controllerClass)
      .getOrElse(super.getControllerInstance(controllerClass))

}