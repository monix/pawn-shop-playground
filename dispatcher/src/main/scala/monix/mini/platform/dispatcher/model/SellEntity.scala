package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.Sell

case class SellEntity(clientId: String, itemId: String, price: Long, date: String, profit: Long) {
  def toProto: Sell = Sell(clientId = clientId, itemId = itemId, price = price, date = date, profit = profit)
}

object SellEntity {
  def fromProto(sell: Sell): SellEntity = {
    SellEntity(clientId = sell.clientId, itemId = sell.itemId, price = sell.price, date = sell.date, profit = sell.profit)
  }
}

