package monix.mini.platform

import monix.execution.Scheduler

package object dispatcher {

  val kafkaIo = Scheduler.io("kafka-io")
}
