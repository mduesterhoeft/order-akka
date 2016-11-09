package com.example

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.persistence.Persistence
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.scalac.amqp.{Connection, ConnectionSettings}
import spray.json._
import DefaultJsonProtocol._
import com.example.OrderActor.{Order, OrderFlow}

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
      .map(_.message)
      .via(messageToOrderFlow())
      .to(Sink.foreach(println)).run

  system.awaitTermination()
}