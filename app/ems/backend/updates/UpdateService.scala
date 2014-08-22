package ems.backend.updates

import scala.util.{Try, Success}

import scaldi.akka.AkkaInjectable
import scaldi.Injector
import akka.actor.{ActorSystem, ActorRef}

import ems.models._


/**
 * A trait that eases access to UpdatesServiceActor
 */
class UpdateService(implicit inj: Injector) extends AkkaInjectable {

  implicit val system = inject[ActorSystem]

  /**
   * The only place where we inject this stateful's actor ref,
   * since scaldi akka integration always creates new instances of actors, to respect the akka actor hierarchy.
   *
   * (see https://github.com/scaldi/scaldi-website/blob/master/learn.md)
   */
  val updatesServiceActor: ActorRef = injectActorRef[UpdatesServiceActor]

}










