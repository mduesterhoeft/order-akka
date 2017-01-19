package com.example

import akka.actor.ActorSystem
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.persistence.Persistence
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import akka.stream.scaladsl.Sink
import com.example.OrderActor._
import com.typesafe.config.ConfigFactory

object  ApplicationMain extends App with OrderFlow with OrderApi {
  val config = ConfigFactory.load()

  implicit val system = ActorSystem("order", config)

  val localSystem = ActorSystem("local", config.getConfig("local"))
  implicit val mat = ActorMaterializer()(localSystem)

  Persistence(system)

  override val orderActor = ClusterSharding(system).start(
    typeName = OrderActor.shardName,
    entityProps = OrderActor.props,
    settings = ClusterShardingSettings(system),
    extractShardId = OrderActor.extractShardId,
    extractEntityId = OrderActor.extractEntityId)

  val orderQueue = "order-akka.queue"
  BindingDeclaration(orderQueue, "order.exchange").withRoutingKey("#")
  val queueDeclaration = QueueDeclaration(orderQueue).withDurable(true).withAutoDelete(false)

  val amqpSource = AmqpSource(
    NamedQueueSourceSettings(AmqpConnectionDetails("192.168.99.100", 5672), orderQueue).withDeclarations(queueDeclaration),
    bufferSize = 10
  )

  amqpSource
    .log("order.queue")
    .via(deliveryToCreateOrderFlow())
    .to(Sink.actorRef[CreateOrder](orderActor, "done"))
    .run()

  Http().bindAndHandle(orderRoutes, "localhost", 8070)
}