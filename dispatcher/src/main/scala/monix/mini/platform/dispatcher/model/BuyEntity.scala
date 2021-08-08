package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.Buy

case class BuyEntity(clientId: String, itemId: String, price: Long, date: String) {
  def toProto: Buy = Buy(clientId = clientId, itemId = itemId, price = price, date = date)
}

object BuyEntity {
  def fromProto(buy: Buy): BuyEntity = {
    BuyEntity(clientId = buy.clientId, itemId = buy.itemId, price = buy.price, date = buy.date)
  }
}

