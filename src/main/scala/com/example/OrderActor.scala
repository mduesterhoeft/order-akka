package com.example

import java.util.UUID

import akka.NotUsed
import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpCharsets
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.stream.scaladsl.{Flow, GraphDSL, Sink}
import akka.util.ByteString
import com.example.OrderActor.{Order, Product}
import io.scalac.amqp.{Delivery, Message}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonParser, RootJsonFormat}

import scala.Product

class OrderActor extends PersistentActor with ActorLogging {
  import OrderActor._

  var state: Option[Order] = None

  override def receiveRecover: Receive = {
    case event: OrderEvent => updateState(event)
    case SnapshotOffer(_, snapshot: Option[Order]) => state = snapshot
  }

  override def receiveCommand: Receive = {
    case CreateOrder(_, order) =>
      log.info(s"received CreateOrder command $order")
      persist(OrderCreated(order))(updateState)
    case SetOrderStatus(_, orderStatus, comment) => persist(OrderStatusSet(orderStatus, comment))(updateState)
    case GetOrder(_) => sender() ! state
  }

  override def persistenceId: String = s"orders-${self.path.name}"

  def updateState(event: OrderEvent): Unit = {
    event match {
      case OrderCreated(order) => state = Some(order)
      case OrderStatusSet(orderStatus: OrderStatus, comment: String) => state = state.map(o => o.copy(orderStatus = orderStatus))
    }
  }
}

object OrderActor {

  val name = "order-actor"
  val props = Props[OrderActor]
  trait OrderCommand {
    def id: String
  }
  trait OrderEvent {}

  sealed trait OrderStatus
  case object Open extends OrderStatus
  case object Complete extends OrderStatus
  case object Cancelled extends OrderStatus

  case class Price(currency: String, amount: BigDecimal)
  case class Product(id: String, name: String, price: Price)
  case class Order(id: String, lineItems: List[Product] = List.empty, total: Price, orderStatus: OrderStatus = Open)

  case class CreateOrder(id: String, order: Order) extends OrderCommand
  case class SetOrderStatus(id: String, orderStatus: OrderStatus, comment: String) extends OrderCommand
  case class GetOrder(id: String)

  case class OrderCreated(order: Order) extends OrderEvent
  case class OrderStatusSet(orderStatus: OrderStatus, comment: String) extends OrderEvent

  trait OrderFlow  {
    import OrderProtocols._

    def deliveryToCreateOrderFlow(): Flow[Delivery, CreateOrder, NotUsed] = {
      Flow[Delivery]
        .map(delivery => delivery.message)
        .map(message => JsonParser(ByteString(message.body.toArray).decodeString("UTF-8")).convertTo[Order])
        .map(o => CreateOrder(UUID.randomUUID().toString, o))
    }
  }
}


