import java.io.File
import camel.CamelConfig
import play.api._
import com.typesafe.config.ConfigFactory

object Global extends GlobalSettings {

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.${mode.toString.toLowerCase}.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }

  override def onStart(app: Application) {
    Logger.info("Application has started")
    CamelConfig.createActor
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }
}