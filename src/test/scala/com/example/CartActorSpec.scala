package com.example

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.example.OrderActor._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class CartActorSpec extends TestKit(ActorSystem("CartActorSpec", ConfigFactory.load().getConfig("localTest")))
  with FlatSpecLike with ImplicitSender with Matchers with BeforeAndAfterAll {

  "OrderActor" should "create order" in {
    val orderActor = system.actorOf(Props[OrderActor])

    val order: Order = givenOrder

    orderActor ! CreateOrder("1", order)

    orderActor ! GetOrder("1")

    expectMsg(Some(order))
  }


  it should "return None in initial state" in {
    val orderActor = system.actorOf(Props[OrderActor])

    orderActor ! GetOrder("1")

    expectMsg(None)
  }

  it should "set order status" in {
    val orderActor = system.actorOf(Props[OrderActor])

    val order = givenOrder
    val id = UUID.randomUUID().toString
    orderActor ! CreateOrder(id, order)
    orderActor ! SetOrderStatus(id, Complete, "all done here")
    orderActor ! GetOrder(id)

    expectMsg(Some(order.copy(orderStatus = Complete)))
  }

  def givenOrder: Order = {
    Order("1",
      List(
        Product("99", "some", Price("EUR", BigDecimal(10)))
      ), Price("EUR", BigDecimal(10))
    )
  }

  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

}
