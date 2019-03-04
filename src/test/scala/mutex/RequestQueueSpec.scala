package mutex

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActorRef, TestKit}
import common.Timestamp
import mutex.Protocol._
import org.scalatest._

import scala.collection.mutable

class RequestQueueSpec extends TestKit(ActorSystem("request-queue-test"))
  with FlatSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  behavior of "request queue"

  var actor: TestActorRef[Process] = _

  var peers: Set[ActorRef] = _

  override protected def beforeEach(): Unit = {
    super.beforeAll()

    actor = TestActorRef(Process.props(0))

    val tmp = mutable.Set[ActorRef]()
    (1 to 3) foreach { i =>
      val peer = system.actorOf(Process.props(i))
      tmp.add(peer)
    }
    peers = tmp.toSet
    actor ! SetPeers(peers)
  }

  it should "able to get resource" in {
    val queue = actor.underlyingActor.requestQueue

    // queue里面有一个Tm:Pi在所有其他request之前(<==关系)
    val Tm = actor.underlyingActor.getAndIncrementTimestamp()
    queue.enqueue(Request(actor, Tm))

    // Pi收到了来自所有其他process时间大于Tm的消息
    peers.zipWithIndex.foreach {
      case (peer, i) => queue.enqueue(Ack(peer, Tm + i + 1))
    }

    actor.underlyingActor.canGetResource should be(true)
  }

  it should "not be able to get resource, when not getting message from one of it's peer" in {
    val queue = actor.underlyingActor.requestQueue

    // queue里面有一个Tm:Pi在所有其他request之前(<==关系)
    val Tm = actor.underlyingActor.getAndIncrementTimestamp()
    queue.enqueue(Request(actor, Tm))

    // Pi收到了来自所有其他process时间大于Tm的消息，有一个没收到
    peers.zipWithIndex.foreach {
      case (_, i) if i == 0 => ()
      case (peer, i) => queue.enqueue(Ack(peer, Tm + i + 1))
    }

    // TODO: 出现heisenbug的位置
    actor.underlyingActor.canGetResource should be(false)
  }

  "when release resource, the second Request" should "in the front" in {
    val queue = actor.underlyingActor.requestQueue

    val Tm = actor.underlyingActor.getAndIncrementTimestamp()
    queue.enqueue(Request(actor, Tm))

    // Pi收到了来自所有其他process时间大于Tm的消息，有一个是请求资源
    var secondRequest: Request = null
    var releaseTime: Timestamp = null
    peers.zipWithIndex.foreach {
      case (peer, i) if i == 0 =>
        secondRequest = Request(peer, Tm + i + 1)
        queue.enqueue(secondRequest)
      case (peer, i) =>
        releaseTime = Tm + i + 1
        queue.enqueue(Ack(peer, releaseTime))
    }

    // actor还是可以拿到资源
    actor.underlyingActor.canGetResource should be(true)
    // 释放之后，队列的第一个应该是secondRequest
    actor.underlyingActor.filterQueue(actor)
    actor.underlyingActor.requestQueue.head should be(secondRequest)
    // actor不能拿资源了
    actor.underlyingActor.canGetResource should be(false)
  }

}
