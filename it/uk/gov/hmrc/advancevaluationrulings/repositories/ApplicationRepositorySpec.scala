package uk.gov.hmrc.advancevaluationrulings.repositories

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.models.application.Application
import uk.gov.hmrc.advancevaluationrulings.models.application.ApplicationId
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[Application]
    with OptionValues
    with ScalaFutures {

  protected override val repository = new ApplicationRepository(mongoComponent)

  ".set" - {

    "must insert an application" in {

      val application = Application(ApplicationId(1), "eori", Instant.now, Instant.now)

      repository.set(application).futureValue
    }

    "must fail to insert a duplicate application" in {

      val application = Application(ApplicationId(1), "eori", Instant.now, Instant.now)

      repository.set(application).futureValue
      repository.set(application).failed.futureValue
    }
  }

  ".get" - {

    "must return an application when one exists" in {

      val application = Application(ApplicationId(1), "eori", Instant.now, Instant.now)

      insert(application).futureValue
      val result = repository.get(ApplicationId(1)).futureValue
      result.value mustEqual application
    }

    "must return None when an application does not exist" in {

      val result = repository.get(ApplicationId(1)).futureValue
      result must not be defined
    }
  }
}
