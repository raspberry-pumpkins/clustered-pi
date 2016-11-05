package clusteredpi.actor

import akka.actor.{Actor, ActorLogging, ActorRef, RootActorPath, Terminated}
import akka.cluster.{Cluster, Member}
import akka.cluster.ClusterEvent._
import akka.routing._
import clusteredpi.model.{AddAsRoutableNode, PeerNodeInformation, SendPeerNodeInformation}

import scala.concurrent.duration._
import scala.collection.immutable.IndexedSeq
import scala.language.postfixOps

class PiClusterMember extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent], classOf[ClusterDomainEvent])

  override def postStop(): Unit = cluster.unsubscribe(self)

  private def makeRouter(routees: IndexedSeq[Routee]) = Router(RoundRobinRoutingLogic(), routees)

  def receive = makeReceive(makeRouter(IndexedSeq.empty))

  private def makeReceive(router: Router): Receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)
      register(member)

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

    case state: CurrentClusterState =>
      log.info(s"Current cluster members: ${state.members}")

    case AddAsRoutableNode() if !router.routees.contains(sender()) =>
      val senderRef = sender()
      log.info("Adding as routable node: {}", senderRef.path.toString)
      context watch senderRef
      context.become(makeReceive(router.addRoutee(ActorRefRoutee(senderRef))))

    case SendPeerNodeInformation() => router.route(getRuntimeInfo, self)

    case runtimeInfo: PeerNodeInformation =>
      log.info(
        "Got runtime information from {}. Details: Num CPUs: {}, Free Mem (MB): {}, Total Mem (MB): {}",
        sender().path.address,
        runtimeInfo.numCpus,
        bytesToMB(runtimeInfo.freeMem),
        bytesToMB(runtimeInfo.totalMem)
      )

    case Terminated(a) =>
      log.warning("Removing terminated node: {}", a.path.address)
      context.become(makeReceive(router.removeRoutee(a)))
  }

  private def register(member: Member): Unit =
    context.actorSelection(
      RootActorPath(member.address) / "user" / "clusterNode"
    ) ! AddAsRoutableNode()

  private def getRuntimeInfo = PeerNodeInformation(
    Runtime.getRuntime.availableProcessors(),
    Runtime.getRuntime.freeMemory(),
    Runtime.getRuntime.totalMemory()
  )

  private def bytesToMB(bytes: Long): Int = {
    // SI convertion method
    val unit: Int = 1024
    // We lose precision all over the place but we don't really care here
    val exp: Int = (Math.log(bytes) / Math.log(unit)).toInt
    (bytes / Math.pow(unit, exp)).toInt
  }

  context.system.scheduler.schedule(10 seconds, 5 seconds) {
    self ! SendPeerNodeInformation()
  }(context.system.dispatcher)


}
