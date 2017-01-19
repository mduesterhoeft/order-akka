package com.example

import akka.NotUsed
import akka.actor.{ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.stream.alpakka.amqp.IncomingMessage
import akka.stream.scaladsl.Flow
import spray.json.JsonParser

import scala.util.{Failure, Success, Try}

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
  def shardName = "order"
  val numberOfShards = 100
  val props = Props[OrderActor]

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case cmd: OrderCommand => (cmd.id, cmd)
  }

  val extractShardId: ShardRegion.ExtractShardId = {
    case cmd: OrderCommand => (math.abs(cmd.id.hashCode()) % numberOfShards).toString
  }

  trait OrderCommand {
    def id: String
  }
  trait OrderEvent {}

  sealed trait OrderStatus {
    val name: String
  }
  case object Open extends OrderStatus {
    val name = "open"
  }
  case object Complete extends OrderStatus {
    val name = "complete"
  }
  case object Cancelled extends OrderStatus {
    val name = "cancelled"
  }

  object OrderStatus {
    def apply(str: String): Try[OrderStatus] = str match {
      case Open.name => Success(Open)
      case Complete.name => Success(Complete)
      case Cancelled.name => Success(Cancelled)
      case _ => Failure(new MatchError(s"Unknown order status '$str'"))
    }

    def unapply(str: String): Option[OrderStatus] = apply(str) match {
      case Success(status) => Some(status)
      case _ => None
    }
  }

  case class Price(currency: String, amount: BigDecimal)
  case class Product(id: String, name: String, price: Price)
  case class Order(id: String, lineItems: List[Product] = List.empty, total: Price, orderStatus: OrderStatus = Open)

  case class CreateOrder(id: String, order: Order) extends OrderCommand
  case class SetOrderStatus(id: String, orderStatus: OrderStatus, comment: String) extends OrderCommand
  case class GetOrder(id: String) extends OrderCommand

  case class OrderCreated(order: Order) extends OrderEvent
  case class OrderStatusSet(orderStatus: OrderStatus, comment: String) extends OrderEvent

  trait OrderFlow extends OrderProtocols {

    def deliveryToCreateOrderFlow(): Flow[IncomingMessage, CreateOrder, NotUsed] = {
      Flow[IncomingMessage]
        .map(incomingMessage => incomingMessage.bytes)
        .map(message => JsonParser(message.utf8String).convertTo[Order])
          .log("order")
        .map(o => CreateOrder(o.id, o))
    }
  }
}


