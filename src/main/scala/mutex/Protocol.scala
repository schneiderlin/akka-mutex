package mutex

import akka.actor.ActorRef
import common.Timestamp

/**
  * Timer
  * 收到什么消息increment timer？
  * - WithTimestamp的消息就increment timer
  * - 发送方加一个参数，表示要不要increment
  *
  * 一个Trait，类似FSM，在Actor收到WithTimestamp消息的时候，拆开消息，普通消息走普通方式，timestamp自动累加
  * 发消息的时候，自动增加timer
  *
  *
  * Timer => Ordering
  * 当收到Timer消息的时候，自动回复一个Ack
  * 有一个queue，自动管理ordering的东西
  *
  * MVP，保存一个queue，这个queue只自动装信息
  *
  * 提供一个isFirst接口
  * 提供一个onFirst callback？
  * 当onFirst的时候，收到一个Message？
  * - mutex在onFirst的时候获取资源
  * - m2o在onFirst的时候发送消息给one
  *
  *
  * m2o 在queue里面站着，直到保证消息发了出去
  *
  * find first ???? in queue
  *
  */
object Protocol {

  case class Request(actor: ActorRef)

  case class Release(actor: ActorRef)

  case class Ack(actor: ActorRef)

  case class SetPeers(peers: Set[ActorRef])

  case object ClientRequest

  case object ClientRelease

}
