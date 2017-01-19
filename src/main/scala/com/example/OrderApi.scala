package com.example


import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{NotFound, NoContent}
import akka.http.scaladsl.server.Directives._
import akka.pattern._
import akka.util.Timeout
import com.example.OrderActor._
import com.example.OrderApi.SetOrderStatusRequest

import scala.concurrent.Future
import scala.concurrent.duration._

trait OrderApiProtocols extends SprayJsonSupport with OrderProtocols {
  implicit val orderStatusRequestFormat = jsonFormat2(SetOrderStatusRequest)
}

trait OrderApi extends OrderApiProtocols {

  val orderActor: ActorRef
  implicit val timeout: Timeout = 10 seconds
//  implicit val ec = ExecutionContext.global

  val orderRoutes = logRequestResult("orders") {
    pathPrefix("orders") {
      pathPrefix(Segment) { id =>
        get {
          val orderFuture: Future[Option[Order]] = orderActor.ask(GetOrder(id)).mapTo[Option[Order]]
          onSuccess(orderFuture) {
            case Some(order) => complete(order)
            case None => complete(NotFound)
          }
        } ~
          put {
            path("order-status") {
              entity(as[SetOrderStatusRequest]) { statusRequest =>
                orderActor ! SetOrderStatus(id, statusRequest.status, statusRequest.comment)
                complete(NoContent)
              }
            }
          }
      }
    }
  }
}

object OrderApi {

  case class SetOrderStatusRequest(status: OrderStatus, comment: String)

}
