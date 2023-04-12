package uk.gov.hmrc.advancevaluationrulings.repositories

import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.models.application.{CounterId, CounterWrapper}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.ExecutionContext.Implicits.global

class CounterRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CounterWrapper]
    with OptionValues
    with ScalaFutures
    with BeforeAndAfterEach {

  protected override val repository = new CounterRepository(mongoComponent)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.seed.futureValue
  }

  "on startup" - {

    "must insert seed records when they do not already exist" in {

      findAll().futureValue must contain theSameElementsAs repository.seeds
    }
  }

  ".seed" - {

    "must not fail when records already exist" in {

      repository.seed.futureValue

      findAll().futureValue must contain theSameElementsAs repository.seeds
    }
  }

  ".nextId" - {

    "must return sequential ids from the correct record" in {

      val applicationIdStartingValue = repository.seeds.find(_._id == CounterId.ApplicationId).head.index
      val attachmentIdStartingValue = repository.seeds.find(_._id == CounterId.AttachmentId).head.index

      repository.nextId(CounterId.ApplicationId).futureValue mustEqual applicationIdStartingValue + 1
      repository.nextId(CounterId.ApplicationId).futureValue mustEqual applicationIdStartingValue + 2
      repository.nextId(CounterId.ApplicationId).futureValue mustEqual applicationIdStartingValue + 3

      repository.nextId(CounterId.AttachmentId).futureValue mustEqual attachmentIdStartingValue + 1
      repository.nextId(CounterId.AttachmentId).futureValue mustEqual attachmentIdStartingValue + 2
      repository.nextId(CounterId.AttachmentId).futureValue mustEqual attachmentIdStartingValue + 3
    }
  }
}
