package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.Pawn

case class PawnEntity(clientId: String, itemId: String, price: Long, tax: Int, limitDays: Int, profit: String) {
  def toProto: Pawn = Pawn(clientId = clientId, itemId = itemId, price = price, tax = tax)
}

object PawnEntity {
  def fromProto(pawn: Pawn): PawnEntity = {
    PawnEntity(clientId = pawn.clientId, itemId = pawn.itemId, price = pawn.price, tax = pawn.tax, limitDays = 1, profit = "")
  }
}