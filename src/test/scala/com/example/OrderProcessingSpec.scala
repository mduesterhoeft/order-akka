package com.example

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.IncomingMessage
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import com.example.OrderActor._
import com.google.common.net.MediaType.JSON_UTF_8
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.BasicProperties
import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpecLike, Matchers}

import scala.collection.immutable._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class OrderProcessingSpec extends TestKit(ActorSystem("CartActorSpec", ConfigFactory.load().getConfig("localTest")))
  with FlatSpecLike with Matchers with OrderActor.OrderFlow {

  implicit val mat = ActorMaterializer()
  "OrderProcessing" should "deserialize json with default order status" in {
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
        |}
        |}""".stripMargin
    val future: Future[Seq[CreateOrder]] = whenOrderProcessed(json)

    val result = Await.result(future, 1 seconds)

    result should have size(1)
    result.head.order should equal(Order("1", List(Product("2", "some", Price("EUR", BigDecimal(13.50)))), Price("EUR", BigDecimal(13.50))))
    result.head.id should not be null
  }

  it should "deserialize json with given order status" in {
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
        |"orderStatus":"complete"
        |}""".stripMargin
    val future: Future[Seq[CreateOrder]] = whenOrderProcessed(json)

    val result = Await.result(future, 1 seconds)

    result should have size 1
    result.head.order should equal(Order("1", List(Product("2", "some", Price("EUR", BigDecimal(13.50)))), Price("EUR", BigDecimal(13.50)), Complete))
    result.head.id should not be null
  }

  it should "fail to deserialize order status with invalid status" in {
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
        |"orderStatus":"Complete"
        |}""".stripMargin
    val future: Future[Seq[CreateOrder]] = whenOrderProcessed(json)

    assertThrows[MatchError] {
      Await.result(future, 1 seconds)
    }
  }

  private def whenOrderProcessed(json: String) = {
    val future = Source(Seq(IncomingMessage(ByteString(json), null, new BasicProperties())))
      .via(deliveryToCreateOrderFlow())
      .runWith(Sink.seq)
    future
  }
}
