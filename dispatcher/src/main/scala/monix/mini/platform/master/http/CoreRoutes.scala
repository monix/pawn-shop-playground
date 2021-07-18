package monix.mini.platform.master.http

import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.date
import org.http4s.circe.{ jsonEncoderOf, jsonOf }
import com.typesafe.scalalogging.LazyLogging
import fs2.{ Chunk, Stream }
import io.circe.Json
import io.circe.generic.auto._
import org.http4s.{ HttpRoutes, QueryParamDecoder, Response }
import monix.eval.Task
import monix.mini.platform.master.Dispatcher
import monix.mini.platform.protocol.{ Category, FetchByCategoryRequest, FetchByIdRequest, FetchByStateRequest, Item, Pawn, Sell, State }
import org.http4s.dsl.Http4sDsl
import io.circe.syntax._
import CoreRoutes._
import monix.mini.platform.master.kafka.KafkaPublisher
import scala.language.implicitConversions

trait CoreRoutes extends Http4sDsl[Task] with LazyLogging {

  val dispatcher: Dispatcher
  implicit val itemPublisher: KafkaPublisher[Item]

  implicit val itemEncoder = jsonOf[Task, ItemEntity]

  object ItemIdQueryParamMatcher extends QueryParamDecoderMatcher[String]("id")

  implicit val categoryQueryParamDecoder: QueryParamDecoder[Category] = QueryParamDecoder[String].map(Category.fromName)
  object CategoryQueryParamMatcher extends QueryParamDecoderMatcher[Category]("category")

  object LimitQueryParamMatcher extends QueryParamDecoderMatcher[Int]("limit")

  implicit val stateQueryParamDecoder: QueryParamDecoder[State] = QueryParamDecoder[String].map(State.fromName)
  object StateQueryParamMatcher extends QueryParamDecoderMatcher[State]("state")

  lazy val routes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case _@ GET -> Root => Ok("Monix Mini Platform")

    case req@ POST -> Root / "item" / "add" => {
      val buyEvent: Task[Item] = req.as[ItemEntity].map(_.toProto)
      logger.info(s"Received Buy Item event.")
      buyEvent.flatMap(dispatcher.publish(_, retries = 3))
              .redeem(ex => {
                logger.error(s"Failed to add new Item.", ex)
                Response(status = InternalServerError)
              }, _ => Response(status = Ok)
      )
    }

    case _@ GET -> Root / "item" / "fetch" :? ItemIdQueryParamMatcher(itemId) =>
      logger.debug(s"Fetch branches request received.")
      for {
        maybeItem <- dispatcher.fetchItem(FetchByIdRequest.of(itemId)).map(_.item)
        httpResponse <- Task.eval {
          maybeItem match {
            case Some(item) => Response(Ok, body = Task.eval(item.toByteArray).toByteStream)
            case None => Response(NotFound, body = Task.now("Item not found.".getBytes).toByteStream)
          }
        }
      } yield httpResponse

    case _@ GET -> Root / "item" / "fetch" :? CategoryQueryParamMatcher(category) :? LimitQueryParamMatcher(limit)=>
      logger.debug(s"Fetch request by $category category received.")
      for {
        items <- dispatcher.fetchItem(FetchByCategoryRequest.of(category, limit))
        httpResponse <- Task.eval {
          items.items.toList match {
            case Nil => Response(NotFound, body = Task.now(s"No items found for category ${category}.".getBytes).toByteStream)
            case _ =>  Response(Ok, body = Task.eval(items.toByteArray).toByteStream)
          }
        }
      } yield httpResponse

    case _@ GET -> Root / "item" / "fetch" :? StateQueryParamMatcher(state) :? LimitQueryParamMatcher(limit)  =>
      logger.debug(s"Fetch request by $state state received.")
      for {
        items <- dispatcher.fetchItem(FetchByStateRequest.of(state, limit))
        httpResponse <- Task.eval {
          items.items.toList match {
            case Nil => Response(NotFound, body = Task.now(s"No items with state `${state} was found.".getBytes).toByteStream)
            case _ =>  Response(Ok, body = Task.eval(items.toByteArray).toByteStream)
          }
        }
      } yield httpResponse
  }

}

object CoreRoutes {
  case class ItemEntity(id: String,
                        name: String,
                        category: Category,
                        price: Long = 0,
                        state: State,
                        ageInMonths: Int) {
    def toProto: Item = Item(id = id, name = name, category = category, price = price, ageInMonths = ageInMonths)
  }
  case object ItemEntity {
    def fromProto(item: Item):ItemEntity = ItemEntity(
      id = item.id,
      name = item.name,
      category = item.category,
      price = item.price,
      state = item.state,
      ageInMonths = item.ageInMonths)
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


