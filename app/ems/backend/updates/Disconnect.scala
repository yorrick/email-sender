package ems.backend.updates

import akka.actor.ActorRef
import ems.models.User

/**
 * Created by yorrick on 14-08-22.
 */
case class Disconnect(user: User, outActor: ActorRef)
