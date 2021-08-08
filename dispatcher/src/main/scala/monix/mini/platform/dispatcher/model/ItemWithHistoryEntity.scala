package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.{ Buy, Item, Pawn, Sell }

case class ItemWithHistoryEntity(item: ItemEntity, buys: Seq[BuyEntity], sells: Seq[SellEntity], pawns: Seq[PawnEntity])

object ItemWithHistoryEntity {
  def fromProto(item: Item, buys: Seq[Buy], sells: Seq[Sell], pawns: Seq[Pawn]): ItemWithHistoryEntity = {
    ItemWithHistoryEntity(
      ItemEntity.fromProto(item),
      buys.map(BuyEntity.fromProto),
      sells.map(SellEntity.fromProto),
      pawns.map(PawnEntity.fromProto))
  }
}

