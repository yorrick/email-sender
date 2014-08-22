package ems.backend.updates

import scala.util.{Try, Success}

import scaldi.akka.AkkaInjectable
import scaldi.Injector
import akka.actor.{ActorSystem, ActorRef}

import ems.models._


/**
 * A trait that eases access to UpdatesServiceActor
 */
trait WithUpdateService extends AkkaInjectable {

  implicit val inj: Injector
  implicit val system: ActorSystem
  def updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Forwarding], Unit] = {
    case Success(forwarding) =>
      updatesServiceActor ! forwarding
  }

}










