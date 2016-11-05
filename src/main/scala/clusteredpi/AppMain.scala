package clusteredpi

import akka.actor.{ActorSystem, Props}
import clusteredpi.actor.PiClusterMember
import com.typesafe.config.ConfigFactory


object AppMain extends App {

  /**
    * Args:
    *   1) Seed node 1
    *   2) Seed node 2
    *   3) This node's IP address/hostname
    *   4) This node's port
    */

  if(args.length == 4) {
    val seedNode1 = args(0)
    val seedNode2 = args(1)
    val myHostname = args(2)
    val myPort = args(3)
    val actorSystemName = "PiCluster"
    val seedNodeURIPrefix = s"akka.tcp://$actorSystemName"

    val seedNodes = List(s""""$seedNodeURIPrefix@$seedNode1"""", s""""$seedNodeURIPrefix@$seedNode2"""").mkString(",")
    println(s"Using seed nodes: $seedNodes")

    val config =
      ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$myPort")
        .withFallback(ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$myHostname"))
        .withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]"))
        .withFallback(ConfigFactory.parseString(s"akka.cluster.seed-nodes=[$seedNodes]"))
        .withFallback(ConfigFactory.load("cluster-member.conf"))

    val system = ActorSystem(actorSystemName, config)
    system.actorOf(Props[PiClusterMember], name = "clusterNode")
  }
}

