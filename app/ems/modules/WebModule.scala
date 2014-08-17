package ems.modules

import scaldi.Module
import securesocial.core.RuntimeEnvironment

import ems.controllers.InjectedApplication
import ems.backend.utils.EMSRuntimeEnvironment
import ems.models.User


class WebModule extends Module {

  binding to new InjectedApplication

  bind [RuntimeEnvironment[User]] to EMSRuntimeEnvironment.instance

  bind identifiedBy "my-message" to "web module message"

}
