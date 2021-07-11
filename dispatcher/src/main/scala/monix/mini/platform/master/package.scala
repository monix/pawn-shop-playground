package monix.mini.platform

import monix.execution.Scheduler

package object master {

  val kafkaIo = Scheduler.io("kafka-io")
}
