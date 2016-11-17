package com.example

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.example.OrderActor._
import spray.json.{DefaultJsonProtocol, JsNull, JsString, JsValue, JsonFormat, RootJsonFormat}

object OrderProtocols extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object OrderStatusFormat extends RootJsonFormat[OrderStatus] {
    def write(status: OrderStatus) = JsString(status.name)

    def read(value: JsValue): OrderStatus = value match {
      case JsString(OrderStatus(s)) => s
      case _ => throw new MatchError(s"not a valid OrderStatus $value")
    }
  }

  implicit val priceFormat = jsonFormat2(Price)
  implicit val productFormat = jsonFormat3(Product)
  implicit val orderFormat = orderStatusDefaultJsonFormat(jsonFormat4(Order))

  private def orderStatusDefaultJsonFormat[T](format: RootJsonFormat[T]): RootJsonFormat[T] = new RootJsonFormat[T] {
    override def write(obj: T): JsValue = {
      format.write(obj).asJsObject
    }

    override def read(json: JsValue): T = {
      val order = json.asJsObject
      format.read(order.fields.contains("orderStatus") match {
        case true => order
        case false => order.copy(fields = order.fields.updated("orderStatus", OrderStatusFormat.write(Open)))
      })
    }
  }
}
