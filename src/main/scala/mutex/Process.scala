package mutex

import akka.actor.{Actor, ActorRef, Props}
import common._
import mutex.Protocol._

import scala.collection.mutable

class Process(_priority: Int) extends Actor with Competitor {

  override def priority: Int = _priority

  var peers: Set[ActorRef] = Set()

  var isHoldingResource: Boolean = false

  override def receive: Receive = {
    case SetPeers(actorRefs) =>
      this.peers = actorRefs.filter(_ != self)
    case ClientRequest =>
      if (isHoldingResource) {
        sender ! "already holding resource"
      } else {
        val request = Request(self)
        peers foreach {
          peer => tell(peer, request)
        }
      }
    case ClientRelease =>
      if (!isHoldingResource) {
        sender ! "not holding resource"
      } else {
        isHoldingResource = false
        filterQueue(self)
        peers foreach {
          peer => tell(peer, Release(self))
        }
      }
    // ---------------------------------------------------------
    case Request(actor) =>
      tryGetResource()

    case Release(actor) =>
      filterQueue(actor)
      tryGetResource()

    case Ack(actor) =>
      tryGetResource()
  }

  //  // not pure
  //  def getAndIncrementTimestamp(): Timestamp = {
  //    val old = timestamp
  //    timestamp = old + 1
  //    old
  //  }
  //
  //  // not pure
  //  def updateTimestamp(receive: Timestamp): Timestamp = {
  //    if (receive <== timestamp) {
  //      // ignore
  //    } else {
  //      timestamp = timestamp + (receive.num + Timestamp.delta)
  //    }
  //
  //    timestamp
  //  }

  // not pure
  /**
    * 释放资源，清空queue
    */
  def filterQueue(releaseActor: ActorRef): Unit = {
    requestQueue = requestQueue.filter(_ match {
      case Request(a, _) if a == releaseActor => false
      case _ => true
    })
  }

  // not pure
  def tryGetResource(): Unit = {
    if (canGetResource) {
      isHoldingResource = true
      println(s"${self.path.name} got the resource")
    }
  }

  def canGetResource: Boolean = {
    findFirstRequestInQueue(requestQueue) match {
      case None => false
      case Some(head) =>
        if (head.actor != self) {
          false
        } else {
          // 在m之后，收到所有来自其他process的消息
          val m = head.timestamp
          val allPeers = mutable.Set[ActorRef](peers.toArray: _*)
          requestQueue foreach {
            request => if (m <== request.timestamp) allPeers.remove(request.actor)
          }
          allPeers.isEmpty
        }
    }
  }

  /**
    * not pure
    *
    * 第一个Request前面的内容都是没用的，因为第一个Request的时间是Tm，Tm前面的消息都没用
    */
  def findFirstRequestInQueue(queue: mutable.PriorityQueue[QueueElem]): Option[Request] = {
    queue.dropWhile {
      case elem@QueueElem(id, t, msg) =>
        msg match {
          case r@Request(_) => false
          case _ => true
        }
      case _ => true
    }

    if (queue.nonEmpty) {
      val elem@QueueElem(id, t, r@Request(_)) = requestQueue.dequeue()
      // 放回去
      queue.enqueue(elem)
      Some(r)
    } else {
      None
    }
  }
}

object Process {
  var counter = 0

  def props(priority: Int) = Props(new Process(priority))
}