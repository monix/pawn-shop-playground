import monix.mini.platform.protocol.{ Buy, Category, FetchByCategoryRequest, FetchByIdRequest, FetchByStateRequest, Item, Pawn, Sell, State }

val item = Item("id123", "name123", Category.Motor, 1L, State.Good, 1)
print("Item: \n" + item.toByteArray.toString)
