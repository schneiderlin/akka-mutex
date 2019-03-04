import akka.actor.{ActorSystem, Props}
import akka.routing.RoundRobinGroup
import akka.testkit.{TestKit, TestProbe}
import org.scalatest._

class AAA extends TestKit(ActorSystem("test"))
  with FlatSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  it should "wtf" in {
    val endProbe = TestProbe()
//    val router = system.actorOf(
//      RoundRobinGroup(paths).props(), "groupRouter")
  }
}