package ems.backend.utils


import java.lang.reflect.Constructor

import securesocial.core.RuntimeEnvironment


/**
 * Contains utilities to create managed controllers
 */
trait WithControllerUtils {

  /**
   * Creates a controller and injects the given RuntimeEnvironment
   * @param controllerClass
   * @param runtimeEnv
   * @tparam A
   * @tparam U
   * @return
   */
  def getControllerInstance[A, U](runtimeEnv: RuntimeEnvironment[U])(controllerClass: Class[A]): Option[A] = {
    val instance  = controllerClass.getConstructors.find { c =>
      val params = c.getParameterTypes
      params.length == 1 && params(0) == classOf[RuntimeEnvironment[U]]
    }.map {
      _.asInstanceOf[Constructor[A]].newInstance(runtimeEnv)
    }

    instance
  }
}
