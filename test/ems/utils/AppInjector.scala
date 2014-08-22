package ems.utils

import play.api.Application
import scaldi.Injector
import scaldi.play.ScaldiSupport


/**
* Trait that allows to access application's injector
*/
trait AppInjector {

  /**
    * Get the app's injector
    * @param app
    * @return
    */
  def appInjector(implicit app: Application): Injector = {
    app.global match {
      case scaldiSupportGlobal: ScaldiSupport => scaldiSupportGlobal.injector
      case _ => throw new Exception(s"Could not find injector in global object, does global extends ScaldiSupport?")
    }
  }
}
