package monix.mini.platform.master

import monix.mini.platform.protocol.SlaveProtocolGrpc.SlaveProtocolStub

case class WorkerRef(slaveId: String, stub: SlaveProtocolStub)
