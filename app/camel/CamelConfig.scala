package camel

import akka.actor.{ActorRef, Props}
import akka.actor.Status.Failure
import akka.camel._
import play.api.Logger
import play.libs.Akka


object CamelConfig {

  def createActor: Unit = {
    Logger.debug("--camel--Start--")
    val rabbitMQProducer = Akka.system.actorOf(Props[RabbitMQProducer].withDispatcher("rabbitmq-dispatcher"))
    val fileConsumer = Akka.system.actorOf(Props(classOf[FileConsumer], rabbitMQProducer).withDispatcher("rabbitmq-dispatcher"))
    val rabbitMQConsumer = Akka.system.actorOf(Props(classOf[RabbitMQConsumer]).withDispatcher("rabbitmq-dispatcher"))

    Logger.debug("--camel--End--")
  }

}


class RabbitMQProducer extends Producer with Oneway {
  def endpointUri = "rabbitmq://localhost/testExchange?username=test&password=test&exchangeType=fanout"
}


class RabbitMQConsumer() extends Consumer {
  def endpointUri = "rabbitmq://localhost/testExchange?username=test&password=test&exchangeType=fanout&threadPoolSize=1"

  def receive = {
    case msg: CamelMessage =>
      Logger.debug("Received %s from rabbit MQ".format(msg.bodyAs[String]))
  }
}


class FileConsumer(rabbitMQProducer: ActorRef) extends Consumer {
  def endpointUri = "file:data/input/actor?delete=true"
  override def autoAck = false

  def receive = {
    case msg: CamelMessage =>
      Logger.debug("received %s" format msg.bodyAs[String])
      sender ! Ack

      rabbitMQProducer ! msg
  }
}
