package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.{Buy, Item, Pawn, Sell}

case class ItemWithHistory(item: ItemEntity, buys: List[BuyEntity], sells: List[SellEntity], pawns: List[PawnEntity])

object ItemWithHistory {
  def fromProto(item: Item, buys: List[Buy], sells: List[Sell], pawns: List[Pawn]): ItemWithHistory = {
    ItemWithHistory(
      ItemEntity.fromProto(item),
      buys.map(BuyEntity.fromProto),
      sells.map(SellEntity.fromProto),
      pawns.map(PawnEntity.fromProto)
    )
  }
}

