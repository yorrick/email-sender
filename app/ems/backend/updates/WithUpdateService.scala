package ems.backend.updates

import scala.util.{Try, Success}

import scaldi.{Injectable, Injector}
import akka.actor.{ActorSystem, ActorRef}

import ems.models._


/**
 * A trait that eases access to UpdatesServiceActor
 */
trait WithUpdateService extends Injectable {

  implicit val inj: Injector
  implicit val system: ActorSystem
  val updatesServiceActor: ActorRef = inject[UpdateService].updatesServiceActor

  /**
   * Helper function that can be used in futures
   * @return
   */
  def notifyWebsockets: PartialFunction[Try[Forwarding], Unit] = {
    case Success(forwarding) =>
      updatesServiceActor ! forwarding
  }

}










