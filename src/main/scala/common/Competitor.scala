package common

import akka.actor.ActorRef
import mutex.Protocol.Ack

import scala.collection.mutable

trait Competitor extends Timer {
  var requestQueue: mutable.PriorityQueue[QueueElem] =
    mutable.PriorityQueue.empty[QueueElem](implicitly[Ordering[QueueElem]].reverse)

  override def tell(ref: ActorRef, message: Any): Unit = {
    val queueElem = QueueElem(self, getAndIncrementTimestamp(), message)
    requestQueue.enqueue(queueElem)
    ref ! queueElem
  }

  protected override def aroundReceive(receive: Receive, msg: Any): Unit = {
    msg match {
      case m@QueueElem(identifier, t, message) =>
        updateTimestamp(t)
        requestQueue.enqueue(m)
        tell(identifier, Ack(self))
        // 不需要timer再更新一次时间了，当作普通message处理
        super.aroundReceive(receive, message)
      case _ => super.aroundReceive(receive, _)
    }
  }
}

case class QueueElem(identifier: ActorRef, timestamp: Timestamp, message: Any) extends WithTimestamp

object QueueElem {
  implicit val elemOrdering: Ordering[QueueElem] = Ordering.by(_.timestamp)
}