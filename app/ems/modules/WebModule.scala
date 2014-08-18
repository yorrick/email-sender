package ems.modules

import scaldi.Module
import securesocial.core.RuntimeEnvironment

import ems.backend.utils.EMSRuntimeEnvironment
import ems.models.User


class WebModule extends Module {
  bind identifiedBy "my-message" to "web module message"
  bind [RuntimeEnvironment[User]] to EMSRuntimeEnvironment.instance
}
