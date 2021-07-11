package monix.mini.platform.master

import monix.mini.platform.protocol.WorkerProtocolGrpc.WorkerProtocolStub

case class WorkerRef(slaveId: String, stub: WorkerProtocolStub)
