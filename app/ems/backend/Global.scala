package ems.backend

import java.io.File
import scala.sys.SystemProperties

import com.typesafe.config.ConfigFactory
import scaldi.play.{ControllerInjector, ScaldiSupport}
import play.api._

import ems.modules.WebModule


object Global extends WithGlobal


trait WithGlobal extends GlobalSettings with ScaldiSupport {

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
    super.onStart(app)

    // we periodically ping the client so the websocket connections do not close
    // TODO move this into a service
//    Akka.system.scheduler.schedule(30.second, 30.second, injectActorRef[UpdatesServiceActor], Ping)

    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    super.onStop(app)
  }

  /**
   * Defines scaldi modules
   * @return
   */
  override def applicationModule = new WebModule :: new ControllerInjector
}