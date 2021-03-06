package com.webtrends.harness.component.kafka

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import com.webtrends.harness.component.kafka.actor.OffsetManager
import com.webtrends.harness.component.kafka.config.KafkaTestConfig
import org.junit.runner.RunWith
import org.slf4j.{Logger, LoggerFactory}
import org.specs2.mutable.SpecificationLike
import org.specs2.runner.JUnitRunner
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._


//@RunWith(classOf[JUnitRunner])
class OffsetManagerSpec extends SpecificationLike with NoTimeConversions {
  import OffsetManager._
  protected final val log:Logger = LoggerFactory.getLogger(getClass)

  val c = KafkaTestConfig.config
  implicit val system = ActorSystem("test", c)
  implicit val timeout = 10 seconds

  val helper =  TestUtil.ZkHelper()
  val zkServer = helper.zkServer
  val path = "/offsetTest/lab/H"
  val offsetActor = system.actorOf(OffsetManager.props(path))
  val probe = TestProbe()

  val myPath = "someNode"
  val data = "something"

  helper.ensureZkAvailable()

  Thread.sleep(2000)

  //Disable because this fails during a mvn build intermittently
  args(skipAll = false, sequential = true)


  "Offset Manager " should {

    "store some data " in {
      probe.send(offsetActor, StoreOffsetData("topic1", "cluster", 0, OffsetData(data.getBytes(StandardCharsets.UTF_8), 5L)))

      val result = probe.receiveOne(timeout).asInstanceOf[OffsetDataResponse]

      log.info("Result {}", result)
      result.data must beLeft
      result.data.left.get.asString() must beEqualTo(data)
    }


    "be able to get some data" in {
      probe.send(offsetActor, GetOffsetData("topic1", "cluster", 0))

      val result = probe.receiveOne(timeout).asInstanceOf[OffsetDataResponse]

      result.data must beLeft
      result.data.left.get.asString() must beEqualTo(data)
    }


    "be successful on noNodeException " in {
      probe.send(offsetActor, GetOffsetData("topic2", "cluster2", 0))

      val r = probe.receiveOne(timeout).asInstanceOf[OffsetDataResponse]

      r.data must beLeft
      r.data.left.get.data must be empty
    }
  }

  step {
    system.stop(offsetActor)
    zkServer.stop()
    system.terminate()
  }
}
