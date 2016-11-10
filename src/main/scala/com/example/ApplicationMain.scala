package com.example

import akka.actor.{ActorSystem, Props}
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.example.OrderActor.{CreateOrder, OrderFlow}
import io.scalac.amqp.Connection

object  ApplicationMain extends App with OrderFlow {
  implicit val system = ActorSystem("MyActorSystem")
  implicit val mat = ActorMaterializer()

  Persistence(system)
  val sharedStore = system.actorOf(Props[SharedLeveldbStore], "store")
  SharedLeveldbJournal.setStore(sharedStore, system)

  val orderActor = system.actorOf(OrderActor.props, OrderActor.name)

  val connection = Connection()
  val queue = connection.consume(queue = "order.queue")
  println("up...")
  Source.fromPublisher(queue)
    .log("order.queue")
    .via(deliveryToCreateOrderFlow())
    .to(Sink.actorRef[CreateOrder](orderActor, "done"))

  system.awaitTermination()
}