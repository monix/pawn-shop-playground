package monix.mini.platform.dispatcher.http

import org.http4s.circe.jsonOf
import com.typesafe.scalalogging.LazyLogging
import org.http4s.{ HttpRoutes, QueryParamDecoder, Response }
import monix.eval.Task
import monix.mini.platform.dispatcher.Dispatcher
import monix.mini.platform.protocol.{ Category, FetchByCategoryRequest, FetchByIdRequest, FetchByStateRequest, Item, State }
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import monix.mini.platform.dispatcher.util.Extensions._
import io.circe._
import io.circe.generic.semiauto._
import CoreRoutes._
import monix.mini.platform.dispatcher.kafka.KafkaPublisher
import io.circe.syntax._

import scala.language.implicitConversions
import io.circe._
import scalapb.UnknownFieldSet

trait CoreRoutes extends Http4sDsl[Task] with LazyLogging {

  val dispatcher: Dispatcher
  implicit val itemPublisher: KafkaPublisher[Item]

  implicit val itemDecoder = jsonOf[Task, ItemEntity]

  object IdQueryParamMatcher extends QueryParamDecoderMatcher[String]("id")

  implicit val categoryQueryParamDecoder: QueryParamDecoder[Category] = QueryParamDecoder[String].map(Category.fromName(_).getOrElse(Category.Clothing))

  object CategoryQueryParamMatcher extends QueryParamDecoderMatcher[Category]("category")

  object LimitQueryParamMatcher extends QueryParamDecoderMatcher[Int]("limit")

  implicit val stateQueryParamDecoder: QueryParamDecoder[State] = QueryParamDecoder[String].map(State.fromName(_).getOrElse(State.Good))

  object StateQueryParamMatcher extends QueryParamDecoderMatcher[State]("state")

  implicit val unknownFieldSetEncoder: Encoder[UnknownFieldSet] = Encoder.forProduct1("fields")(_ => Tuple1(Map.empty[String, String]))
  implicit val itemEncoder: Encoder[Item] = Encoder.forProduct7("id", "name", "category", "price", "state", "ageInMonths", "unknownFields") {
    item =>
      (item.id, item.name, item.category, item.price, item.state, item.ageInMonths, item.unknownFields)
  }

  lazy val coreRoutes: HttpRoutes[Task] = HttpRoutes.of[Task] {

    case _@ GET -> Root => Ok("Monix Mini Platform")

    case req @ POST -> Root / "add" => {
      val item: Task[Item] = req.as[ItemEntity].map(_.toProto)
      logger.info(s"Received Add Item.")
      item.flatMap(dispatcher.publish(_, retries = 3))
        .redeem(ex => {
          logger.error(s"Failed to add new Item.", ex)
          Response(status = InternalServerError)
        }, _ => Response(status = Ok))
    }

    case _@ GET -> Root / "fetch" :? IdQueryParamMatcher(itemId) =>

      logger.debug(s"Fetch branches request received.")
      for {
        maybeItem <- dispatcher.fetchItem(FetchByIdRequest.of(itemId)).map(_.item)
        httpResponse <- Task.eval {
          maybeItem match {
            case Some(item) => {
              logger.info(s"Found item: ${item.toProtoString}")
              Response(Ok, body = Task.eval(item.toEntity.asJson).toByteStream)
            }
            case None => Response(NotFound, body = Task.now("Item not found.".getBytes).toByteStream)
          }
        }
      } yield httpResponse

    case _@ GET -> Root / "fetch" :? CategoryQueryParamMatcher(category)  =>
      logger.debug(s"Fetch request by $category category received.")
      for {
        items <- dispatcher.fetchItem(FetchByCategoryRequest.of(category, 10))
        httpResponse <- Task.eval {
          items.items.toList match {
            case Nil => Response(NotFound, body = Task.now(s"No items found with category ${category}.".getBytes).toByteStream)
            case _ => Response(Ok, body = Task.eval(items.items.toEntity.asJson).toByteStream)
          }
        }
      } yield httpResponse

    case _@ GET -> Root / "fetch" :? StateQueryParamMatcher(state)=>
      logger.debug(s"Fetch request by $state state received.")
      for {
        fetchItemsResponse <- dispatcher.fetchItem(FetchByStateRequest.of(state, 10))
        httpResponse <- Task.eval {
          fetchItemsResponse.items.toList match {
            case Nil => Response(NotFound, body = Task.now(s"No items found with state `${state}.".getBytes).toByteStream)
            case _ => Response(Ok, body = Task.eval(fetchItemsResponse.items.toEntity.asJson).toByteStream)
          }
        }
      } yield httpResponse
  }

}

object CoreRoutes {

  object CategoryEnum extends Enumeration {
    type CategoryEnum = Value
    val Reading, Collection, Decoration, Clothing, Jewelry, Motor, Electronics = Value

  }

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

}

