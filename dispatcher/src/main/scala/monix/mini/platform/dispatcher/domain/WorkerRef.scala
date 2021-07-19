package monix.mini.platform.dispatcher.domain

import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocolStub

case class WorkerRef(slaveId: String, stub: WorkerProtocolStub)
