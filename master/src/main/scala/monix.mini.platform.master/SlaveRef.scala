package monix.mini.platform.master

import monix.mini.platform.protocol.SlaveProtocolGrpc.SlaveProtocolStub

case class SlaveRef(slaveId: String, stub: SlaveProtocolStub)
