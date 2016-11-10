package com.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import com.example.OrderActor.{Order, Price, Product}
import com.google.common.net.MediaType.JSON_UTF_8
import com.typesafe.config.ConfigFactory
import io.scalac.amqp.{Delivery, DeliveryTag, Message}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.collection.immutable._
import scala.concurrent.Await
import scala.concurrent.duration._
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
    val future = Source(Seq(Delivery(Message(body = ByteString(json), contentType = Some(JSON_UTF_8)), DeliveryTag(1), "test", "#", true)))
      .via(deliveryToCreateOrderFlow())
      .runWith(Sink.seq)

    val result = Await.result(future, 1 seconds)

    result should have size(1)
    result.head.order should equal(Order("1", List(Product("2", "some", Price("EUR", BigDecimal(13.50)))), Price("EUR", BigDecimal(13.50))))
    result.head.id should not be null
  }
}
