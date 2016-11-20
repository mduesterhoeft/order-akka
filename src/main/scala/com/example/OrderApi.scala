package com.example


import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes.{Accepted, InternalServerError, NotFound, OK}
import com.example.OrderActor._
import akka.pattern._
import akka.util.Timeout
import com.example.OrderApi.SetOrderStatusRequest
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Success

trait OrderApiProtocols extends SprayJsonSupport with OrderProtocols {
  implicit val orderStatusRequestFormat = jsonFormat2(SetOrderStatusRequest)
}

trait OrderApi extends OrderApiProtocols {

  val orderActor: ActorRef
  implicit val timeout: Timeout = 5 seconds
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
                complete(Accepted)
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
