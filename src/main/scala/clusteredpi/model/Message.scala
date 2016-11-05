package clusteredpi.model

case class AddAsRoutableNode()
case class PeerNodeInformation(numCpus: Int, freeMem: Long, totalMem: Long)
case class SendPeerNodeInformation()
