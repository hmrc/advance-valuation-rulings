package uk.gov.hmrc.advancevaluationrulings.repositories

import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.models.application._
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

  private val trader = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
  private val goodsDetails = GoodsDetails("name", "description", None, None, None)
  private val method = MethodOne(None, None, None)
  private val contact = ContactDetails("name", "email", None)

  ".set" - {

    "must insert an application" in {

      val application = Application(
        id = ApplicationId(1),
        applicantEori = "applicantEori",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        created = Instant.now,
        lastUpdated = Instant.now
      )

      repository.set(application).futureValue
    }

    "must fail to insert a duplicate application" in {

      val application = Application(
        id = ApplicationId(1),
        applicantEori = "applicantEori",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        created = Instant.now,
        lastUpdated = Instant.now
      )

      repository.set(application).futureValue
      repository.set(application).failed.futureValue
    }
  }

  ".get" - {

    "must return an application when one exists" in {

      val application = Application(
        id = ApplicationId(1),
        applicantEori = "applicantEori",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        created = Instant.now,
        lastUpdated = Instant.now
      )

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
