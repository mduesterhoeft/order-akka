package com.example

import akka.actor.ActorRef
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes.{Accepted, NotFound, OK}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestKit, TestProbe}
import akka.util.ByteString
import com.example.OrderActor._
import com.example.OrderApi.SetOrderStatusRequest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import spray.json._



class OrderRouteSpec extends FlatSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with OrderApi {
  val testProbe = TestProbe()
  override val orderActor: ActorRef = testProbe.ref

  val order = Order("1",
    List(
      Product("99", "some", Price("EUR", BigDecimal(10)))
    ),
    Price("EUR", BigDecimal(10))
  )

  "OrderApi" should "get order" in {
    givenOrderActorMock()
    Get("/orders/1") ~> orderRoutes ~> check {
      handled shouldBe true
      status shouldBe OK
      responseAs[Order] shouldEqual order
      testProbe.expectMsgType[GetOrder]
    }
  }

  it should "reply with not found on unknown order" in {
    givenOrderActorMock()
    Get("/orders/not-exiting") ~> orderRoutes ~> check {
      handled shouldBe true
      status shouldBe NotFound
      testProbe.expectMsgType[GetOrder]
    }
  }

  it should "set order status" in {
    val request = SetOrderStatusRequest(Cancelled, "some")

    Put("/orders/1/order-status", HttpEntity(`application/json`, ByteString(request.toJson.toString()))) ~> orderRoutes ~> check {
      handled shouldBe true
      status shouldBe Accepted
      testProbe.expectMsg(SetOrderStatus("1", request.status, request.comment))
    }
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  def givenOrderActorMock() = {
    testProbe.setAutoPilot(new TestActor.AutoPilot {
      def run(sender: ActorRef, msg: Any): TestActor.AutoPilot =
        msg match {
          case GetOrder("1") ⇒
            sender ! Some(order)
            TestActor.NoAutoPilot
          case _ =>
            sender ! None
            TestActor.NoAutoPilot
        }
    })
  }
}
