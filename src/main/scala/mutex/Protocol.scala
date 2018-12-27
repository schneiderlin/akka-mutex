package mutex

import akka.actor.ActorRef

object Protocol {

  trait WithTimestamp {
    val timestamp: Timestamp
    val actor: ActorRef
  }

  case class Request(actor: ActorRef, timestamp: Timestamp) extends WithTimestamp

  case class Release(actor: ActorRef, timestamp: Timestamp) extends WithTimestamp

  case class Ack(actor: ActorRef, timestamp: Timestamp) extends WithTimestamp

  object WithTimestamp {
    implicit val timestampOrdering: Ordering[WithTimestamp] = Ordering.by(_.timestamp)

    def unapply(arg: WithTimestamp): Option[(ActorRef, Timestamp)] =
      Some(arg.actor, arg.timestamp)
  }

  case class SetPeers(peers: Set[ActorRef])

  case object ClientRequest

  case object ClientRelease

}
