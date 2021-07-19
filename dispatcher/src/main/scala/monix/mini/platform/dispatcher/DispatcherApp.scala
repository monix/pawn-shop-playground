package monix.mini.platform.dispatcher

import cats.effect.ExitCode
import com.typesafe.scalalogging.LazyLogging
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import monix.mini.platform.dispatcher.config.DispatcherConfig
import monix.mini.platform.dispatcher.grpc.GrpcServer
import monix.mini.platform.dispatcher.http.HttpServer

import scala.concurrent.duration._

object DispatcherApp extends TaskApp with LazyLogging {

  def run(args: List[String]): Task[ExitCode] = {
      Scheduler.global.scheduleWithFixedDelay(5.seconds, 15.seconds)(logger.info("Still alive."))
      val grpcScheduler = Scheduler.fixedPool("grpc-io", 1)
      val httpScheduler = Scheduler.fixedPool("http-io", 1)

      {
          for {
              config <- Task.evalAsync(DispatcherConfig.load()).memoizeOnSuccess
              _ = logger.info(s"Starting master server with config: $config")
              dispatcher: Dispatcher = new Dispatcher(config)
              grpcServer = GrpcServer(dispatcher, config, grpcScheduler)
              httpServer = new HttpServer(config, httpScheduler)
              _ <- Task.race(
                  Task.evalAsync(grpcServer)
                          .map(_.blockUntilShutdown())
                          .guarantee(grpcServer.stop)
                  , httpServer.bindAndNeverTerminate)
          } yield ()
      }.redeem(ex => {
          logger.error(s"Application server error", ex)
          ExitCode.Success
        }, _ => {
          logger.info("The application finished gracefully.")
          ExitCode.Success
        }
      )





  }

}
