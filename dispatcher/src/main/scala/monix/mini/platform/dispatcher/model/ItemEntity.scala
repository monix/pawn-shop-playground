package monix.mini.platform.dispatcher.model

import monix.mini.platform.protocol.{Category, Item, State}

case class ItemEntity(
    id: String,
    name: String,
    category: String,
    price: Long = 0,
    state: String,
    ageInMonths: Int) {
    def toProto: Item = {
      val categoryEnum = Category.fromName(category).getOrElse(Category.Motor)
      val stateEnum = State.fromName(state).getOrElse(State.Good)
      Item(id = id, name = name, category = categoryEnum, price = price, ageInMonths = ageInMonths, state = stateEnum)
    }
  }

case object ItemEntity {
  def fromProto(item: Item): ItemEntity = ItemEntity(
    id = item.id,
    name = item.name,
    category = item.category.toString(),
    price = item.price,
    state = item.state.toString(),
    ageInMonths = item.ageInMonths)
}