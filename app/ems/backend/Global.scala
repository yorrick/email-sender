package ems.backend

import java.io.File
import play.api.mvc.{Filter, Filters, EssentialAction}
import scaldi.Injectable

import scala.sys.SystemProperties

import com.typesafe.config.ConfigFactory
import scaldi.play.{ControllerInjector, ScaldiSupport}
import play.api._

import ems.modules.WebModule


object Global extends WithGlobal


trait WithGlobal extends GlobalSettings with ScaldiSupport with Injectable {

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

  override def doFilter(a: EssentialAction): EssentialAction = {
    val filter = inject[Filter]
    Filters(super.doFilter(a), filter)
  }

  /**
   * Defines scaldi modules
   * @return
   */
  override def applicationModule = new WebModule :: new ControllerInjector
}