# Clustered Pumpkins
This is the second step in our journey to do cool things by combining Pi with Akka/Scala, bringing us closer to making fully interactive pumpkins! This time we get Akka Cluster going with 2 raspberry pi's and my laptop as a 3rd node just to show off the versatility of Akka/Scala. Check out this sweet gif of 2 pi's and a laptop in a clustered setup (The Raspberry pi's are at the top, my laptop is the bottom pane):

![clustered-pi](http://i.imgur.com/N3ZNjIJ.gif "Akka clustering with Rasberry Pi")

## Running the example

### Building a fat jar
The steps to clone/build/assemble the project are identical to the steps outlined in the [hello-akka](https://github.com/rasberry-pumpkins/hello-akka) application so head over there first if you need a refresher on that.

### Starting the cluster
Once you have the fat jar distributed everywhere (ideally you want 2-3 rasberry pi's + maybe a laptop running this) we can start a new node with this command:

```shell
java -jar clustered-pi-assembly-1.0.jar \
  192.168.1.156:2551 \ # First seed node
  192.168.1.198:2551 \ # Second seed node
  192.168.1.198 \ # The IP of the node you are currently SSH'd to
  2551 # The port that you want the node to run on
```

Let's break this down. In Akka clustering, you need at least 2 seed nodes to start the cluster. You tell the node about them by passing them as command line arguments in the for of `hostname:port` (can be an IP address too). The 3rd and 4th arguments is the external hostname/IP address and Port for the node you are currently on. The order you start the nodes doesn't really matter that much since the cluster will self-heal but here's an example:

First I'll login to a Pi with IP 192.168.1.198 and run this (I'll make this guy a seed node):

```shell
java -jar clustered-pi-assembly-1.0.jar \
  192.168.1.156:2551 \ # First seed node (IP of my laptop)
  192.168.1.198:2551 \ # Second seed node (IP of this node)
  192.168.1.198 \
  2551
```

Next, lets start up the second seed node on my laptop (notice how the seed nodes are always the same):

```shell
java -jar clustered-pi-assembly-1.0.jar \
  192.168.1.156:2551 \ # First seed node (IP of my laptop)
  192.168.1.198:2551 \ # Second seed node (IP of initial Pi node)
  192.168.1.156 \
  2551
```

Lastly let's start a node on my 2nd pi (I only have two at them moment):

```shell
java -jar clustered-pi-assembly-1.0.jar \
  192.168.1.156:2551 \ # First seed node (IP of my laptop)
  192.168.1.198:2551 \ # Second seed node (IP of initial Pi node)
  192.168.1.200 \
  2551
```

There you go! If all goes well then you should see cluster log output for all 3 nodes. Make sure to play around by adding/removing nodes and watch the cluster handle the failures and changes in cluster state.