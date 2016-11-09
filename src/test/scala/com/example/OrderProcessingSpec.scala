package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.model.MediaType
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import io.scalac.amqp.{Message, Persistent}
import org.scalatest.{FlatSpec, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import akka.http.scaladsl.model.MediaTypes._
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.example.OrderActor.{CreateOrder, Order, Price, Product}
import com.google.common.net.MediaType.JSON_UTF_8
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
class OrderProcessingSpec extends TestKit(ActorSystem("CartActorSpec", ConfigFactory.load().getConfig("localTest")))
  with FlatSpecLike with Matchers with OrderActor.OrderFlow {

  implicit val mat = ActorMaterializer()
  "OrderProcessing" should "deserialize json" in {
    val json =
      """{
        |"id": "1" ,
        |"lineItems": [
        | {
        |   "id": "2",
        |   "name": "some",
        |   "price" : {
        |     "amount": 13.50,
        |     "currency": "EUR"
        |   }
        | }
        |],
        |"total": {
        |   "amount": 13.50,
        |   "currency": "EUR"
        |},
        |"orderStatus":"Open"
        |}""".stripMargin
    val future = Source(scala.collection.immutable.Seq(
      Message(body = ByteString(json), contentType = Some(JSON_UTF_8))
    ))
      .via(messageToOrderFlow())
      .via(orderToCreateOrderCommand())
      .runWith(Sink.seq)

    val result = Await.result(future, 1 seconds)

    result should have size(1)
    result.head.order should equal(Order("1", List(Product("2", "some", Price("EUR", BigDecimal(13.50)))), Price("EUR", BigDecimal(13.50))))
    result.head.id should not be null
  }
}
