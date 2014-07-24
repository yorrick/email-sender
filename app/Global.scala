import java.io.File
import java.lang.reflect.Constructor

import securesocial.core.authenticator.{HttpHeaderAuthenticatorBuilder, AuthenticatorStore, CookieAuthenticatorBuilder}
import securesocial.core.services.{AuthenticatorService, CacheService}

import scala.collection.immutable.ListMap

import com.typesafe.config.ConfigFactory
import securesocial.core.RuntimeEnvironment
import securesocial.core.providers.GoogleProvider
import securesocial.controllers.ViewTemplates

import play.api._

import ems.backend.{RedisAuthenticatorStore, MyEventListener, MongoDBUserService}
import ems.controllers.EMSViewTemplates
import ems.models.User


object Global extends GlobalSettings {

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.${mode.toString.toLowerCase}.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }

  override def onStart(app: Application) {
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  /**
   * The runtime environment for this sample app.
   */
  object EMSRuntimeEnvironment extends RuntimeEnvironment.Default[User] {
    override lazy val userService: MongoDBUserService = new MongoDBUserService()

    // use AuthenticationStore based on redis (distributed)
    override lazy val authenticatorService = new AuthenticatorService(
//      new CookieAuthenticatorBuilder[User](new RedisAuthenticatorStore(), idGenerator),
//      new HttpHeaderAuthenticatorBuilder[User](new RedisAuthenticatorStore(), idGenerator)
      new CookieAuthenticatorBuilder[User](new RedisAuthenticatorStore(cacheService), idGenerator),
      new HttpHeaderAuthenticatorBuilder[User](new RedisAuthenticatorStore(cacheService), idGenerator)
    )

    override lazy val eventListeners = List(new MyEventListener())

    // override authentication views
    override lazy val viewTemplates: ViewTemplates = new EMSViewTemplates(this)

    override lazy val providers = ListMap(
      include(new GoogleProvider(routes, cacheService, oauth2ClientFor(GoogleProvider.Google)))
    )
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
  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    val instance  = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[User]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(EMSRuntimeEnvironment)
    }
    instance.getOrElse(super.getControllerInstance(controllerClass))
  }

}