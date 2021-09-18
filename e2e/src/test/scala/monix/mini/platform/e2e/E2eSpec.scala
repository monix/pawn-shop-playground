package monix.mini.platform.e2e

import cats.effect.IO
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.flatspec.AnyFlatSpec

import scala.concurrent.ExecutionContext.global
import scala.concurrent.duration.DurationInt
import org.http4s.client.dsl.io._
import org.http4s.client.blaze._
import io.circe.generic.auto._
import io.circe.syntax._
import cats.effect.IO
import fs2.Stream
import org.http4s.{Method, Request, Uri}

class E2eSpec extends AnyFlatSpec {
  case class Id(id: String)
  val cs = IO.contextShift(global)
  implicit val ce = IO.ioConcurrentEffect(cs)
  val httpClient =  BlazeClientBuilder[IO](global).resource
  val addItemUri = Uri.unsafeFromString("localhost:8080/item/add")
  val request = Request[IO](Method.POST, addItemUri, Stream(Id("").asJson))

  "Workers" should "consume from kafka and insert to mongo the `n` items sent though the dispatcher" in {
    val testDuration = 1.minutes

    httpClient.use(client => client.run())


  }
}
