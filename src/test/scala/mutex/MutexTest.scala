package mutex

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestActor, TestActorRef, TestKit, TestProbe}
import mutex.Protocol._
import org.scalatest._
import org.scalatest.prop.PropertyChecks

import scala.collection.mutable

class MutexTest extends TestKit(ActorSystem("mutex-test"))
  with FlatSpecLike with Matchers with BeforeAndAfterAll with PropertyChecks
  with BeforeAndAfterEach {

  behavior of "mutex test"

  val numberOfProcess = 3

  var actorProbe: TestProbe = TestProbe()
  var actor: TestActorRef[Process] = _
  var peerProbe1: TestProbe = TestProbe()
  var peer1: TestActorRef[Process] = _
  var peerProbe2: TestProbe = TestProbe()
  var peer2: TestActorRef[Process] = _
  var peers: Set[ActorRef] = _

  override def beforeAll(): Unit = {
    actor = TestActorRef(Process.props(0), "peer-0")
    actorProbe.setAutoPilot((sender, msg) => msg match {
      case _ =>
        actor.tell(msg, sender)
        TestActor.KeepRunning
    })

    peer1 = TestActorRef(Process.props(1), "peer-1")
    peerProbe1.setAutoPilot((sender, msg) => msg match {
      case _ =>
        peer1.tell(msg, sender)
        TestActor.KeepRunning
    })

    peer2 = TestActorRef(Process.props(2), "peer-2")
    peerProbe2.setAutoPilot((sender, msg) => msg match {
      case _ =>
        peer2.tell(msg, sender)
        TestActor.KeepRunning
    })

    //    val actorPeers: Set[ActorRef] = Set(actorProbe.ref, peerProbe1.ref, peerProbe2.ref)
    peers = Set(actor, peer1, peer2)
  }


  override def afterEach(): Unit = {
    super.afterEach()
    actor ! SetPeers(peers)
    peer1 ! SetPeers(peers)
    peer2 ! SetPeers(peers)
  }

  // 全部都是真的actor，用来测试high level的行为
  def initRealPeers(): Unit = {
    val members: mutable.Set[ActorRef] = mutable.Set()

    (0 to numberOfProcess) foreach { i =>
      val actor = system.actorOf(Process.props(i), s"process-$i")
      members.add(actor)
    }

    members foreach { actor =>
      actor ! SetPeers(members.toSet)
    }
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  it should "send Request with timestamp to all peers, once client request" in {
    val actorPeers: Set[ActorRef] = Set(actorProbe.ref, peerProbe1.ref, peerProbe2.ref)
    actor ! SetPeers(actorPeers)

    actor ! ClientRequest
    peerProbe1.expectMsg(Request(actor, Timestamp(0, 0)))
    peerProbe2.expectMsg(Request(actor, Timestamp(0, 0)))

    peer1.underlyingActor.requestQueue should not be empty
    peer2.underlyingActor.requestQueue should contain(Request(actor, Timestamp(0, 0)))
  }

  it should "update logical timestamp when it receive message from it's peers" in {
    val probe = TestProbe()

    val initialTimestamp = actor.underlyingActor.timestamp

    forAll { time: Timestamp =>
      if (time < initialTimestamp) {
        actor.receive(Request(probe.ref, time))

        actor.underlyingActor.timestamp should be > time
      } else {
        actor.receive(Request(probe.ref, time))

        actor.underlyingActor.timestamp should be > initialTimestamp
        actor.underlyingActor.timestamp should be > time
      }
    }

    forAll { time: Timestamp =>
      if (time < initialTimestamp) {
        actor.receive(Ack(probe.ref, time))

        actor.underlyingActor.timestamp should be > time
      } else {
        actor.receive(Ack(probe.ref, time))

        actor.underlyingActor.timestamp should be > initialTimestamp
        actor.underlyingActor.timestamp should be > time
      }
    }

    forAll { time: Timestamp =>
      if (time < initialTimestamp) {
        actor.receive(Release(probe.ref, time))

        actor.underlyingActor.timestamp should be > time
      } else {
        actor.receive(Release(probe.ref, time))

        actor.underlyingActor.timestamp should be > initialTimestamp
        actor.underlyingActor.timestamp should be > time
      }
    }
  }

  it should "send ack back to sender, once receive Request" in {
    val probe = TestProbe()

    forAll { time: Timestamp =>
      actor.receive(Request(probe.ref, time))
      probe.expectMsgPF() {
        case Ack(_, t) => t should be > time
      }
    }
  }

  it should "get resource when no competitors" in {
    actor ! ClientRequest

    actor.underlyingActor.isHoldingResource should be(true)
  }

  "when a actor is holding resource, other" should "unable to get resource" in {
    actor ! ClientRequest
    actor.underlyingActor.isHoldingResource should be(true)

    peer1 ! ClientRequest
    peer2 ! ClientRequest
    peer1.underlyingActor.isHoldingResource should be(false)
    peer2.underlyingActor.isHoldingResource should be(false)
  }

  "when a actor release resource, the next Request" should "be fulfill" in {
    actor ! ClientRequest
    peer1 ! ClientRequest
    peer2 ! ClientRequest

    actor.underlyingActor.isHoldingResource should be(true)
    peer1.underlyingActor.isHoldingResource should be(false)
    peer2.underlyingActor.isHoldingResource should be(false)

    actor ! ClientRelease
    actor.underlyingActor.isHoldingResource should be(false)
    peer1.underlyingActor.isHoldingResource should be(true)
    peer2.underlyingActor.isHoldingResource should be(false)

    peer1 ! ClientRelease
    actor.underlyingActor.isHoldingResource should be(false)
    peer1.underlyingActor.isHoldingResource should be(false)
    peer2.underlyingActor.isHoldingResource should be(true)
  }
}
