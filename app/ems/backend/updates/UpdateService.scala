package ems.backend.updates

import akka.actor.ActorRef


trait UpdateService {
  def updatesServiceActor: ActorRef
}
