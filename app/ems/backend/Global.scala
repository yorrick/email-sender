package ems.backend

import java.io.File
import scala.sys.SystemProperties

import com.typesafe.config.ConfigFactory
import play.api._
import scaldi.play.{ControllerInjector, ScaldiSupport}

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
    Redis.openConnections
    super.onStart(app)
    Logger.info("Application has started")
  }

  override def onStop(app: Application) {
    Redis.closeConnections
    Logger.info("Application shutdown...")
    super.onStop(app)
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    try {
      super.getControllerInstance(controllerClass)
    } catch {
      case t: Throwable =>
        Logger.warn(s"Could not create controller instance $controllerClass: $t")
        throw t
    }
  }

  /**
   * Defines scaldi modules
   * @return
   */
  override def applicationModule = new WebModule :: new ControllerInjector
}