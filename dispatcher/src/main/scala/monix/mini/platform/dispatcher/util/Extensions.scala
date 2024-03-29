package monix.mini.platform.dispatcher.util

import fs2.{ Chunk, Stream }
import io.circe.Json
import monix.eval.Task
import monix.mini.platform.dispatcher.http.CoreRoutes.ItemEntity
import monix.mini.platform.dispatcher.model.ItemWithHistoryEntity
import monix.mini.platform.protocol.{ Item, ItemWithHistory }

object Extensions {

  implicit class ExtendedItem[A](item: Item) {
    def toEntity: ItemEntity = {
      ItemEntity(item.id, item.name, item.category.toString(), item.price, item.state.toString(), item.ageInMonths)
    }
  }

  implicit class ExtendedWithHistory[A](itemWithHistory: ItemWithHistory) {
    def toEntity: Option[ItemWithHistoryEntity] = {
      itemWithHistory.item.map { item =>
        ItemWithHistoryEntity.fromProto(item, itemWithHistory.buys, itemWithHistory.sells, itemWithHistory.pawns)
      }
    }
  }

  implicit class ExtendedItemsWithHistory[A](itemsWithHistory: Seq[ItemWithHistory]) {
    def toEntity: Seq[ItemWithHistoryEntity] = itemsWithHistory.flatMap(_.toEntity)
  }

  implicit class ExtendedItems[A](items: Seq[Item]) {
    def toEntity: Seq[ItemEntity] = items.map(_.toEntity)
  }

  implicit class ExtendedArrayByteTask[A](task: Task[Array[Byte]]) {
    def toByteStream: Stream[Task, Byte] = {
      Stream.eval(task).flatMap(arr => Stream.chunk(Chunk.array(arr)))
    }
  }

  implicit class ExtendedJsonTask[A](task: Task[Json]) {
    def toByteStream: Stream[Task, Byte] = {
      Stream.eval(task).flatMap(json => Stream.chunk(Chunk.array(json.toString().getBytes)))
    }
  }
}
