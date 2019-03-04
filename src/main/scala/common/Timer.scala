package common

import akka.actor.{Actor, ActorRef}

/**
  * Timer在每次接收到WithTimestamp的消息都会increment timer
  * 有一个tell方法，可以在发送消息之前带上当前timestamp
  */
trait Timer extends Actor {
  def priority: Int

  var timestamp = Timestamp(0, priority)

  def tell(ref: ActorRef, message: Any): Unit = {
    val withTimeMsg = BasicWithTimestamp(getAndIncrementTimestamp(), message)
    ref ! withTimeMsg
  }

  override protected def aroundReceive(receive: Receive, msg: Any): Unit = {
    msg match {
      case WithTimestamp(t, message) => updateTimestamp(t)
      case _ => _
    }
    super.aroundReceive(receive, msg)
  }

  // not pure
  def getAndIncrementTimestamp(): Timestamp = {
    val old = timestamp
    timestamp = old + 1
    old
  }

  // not pure
  def updateTimestamp(receive: Timestamp): Timestamp = {
    if (receive <== timestamp) {
      // ignore
    } else {
      timestamp = timestamp + (receive.num + Timestamp.delta)
    }
    timestamp
  }
}

trait WithTimestamp {
  val timestamp: Timestamp
  val message: Any
}

case class BasicWithTimestamp(timestamp: Timestamp, message: Any) extends WithTimestamp

object WithTimestamp {
  implicit val timestampOrdering: Ordering[WithTimestamp] = Ordering.by(_.timestamp)

  def unapply(arg: WithTimestamp): Option[(Timestamp, Any)] =
    Some(arg.timestamp, arg.message)
}