package com.example

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.example.OrderActor._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

object OrderProtocols extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object OrderStatusFormat extends RootJsonFormat[OrderStatus] {
    //see https://github.com/spray/spray-json/issues/186
    private val mapping = Seq(Open, Complete, Cancelled).map(obj ⇒ key(obj) -> obj).toMap

    def write(c: OrderStatus) = JsString(key(c))

    def read(value: JsValue): OrderStatus = (value match {
      case JsString(value) => mapping.get(value)
      case _ => None
    }).getOrElse(throw new MatchError(s"not a valid OrderStatus $value"))

    def key(obj: OrderStatus): String = {
      obj.toString.toLowerCase
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
