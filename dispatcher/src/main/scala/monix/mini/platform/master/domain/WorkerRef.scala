package monix.mini.platform.master.domain

import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocolStub

case class WorkerRef(slaveId: String, stub: WorkerProtocolStub)
