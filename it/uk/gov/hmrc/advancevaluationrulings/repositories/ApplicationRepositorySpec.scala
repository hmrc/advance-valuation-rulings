package uk.gov.hmrc.advancevaluationrulings.repositories

import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationRepositorySpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[Application]
    with OptionValues
    with ScalaFutures {

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.applicationTtlInDays) thenReturn 1

  protected override val repository: ApplicationMongoRepository =
    new ApplicationMongoRepository(mongoComponent, mockAppConfig)

  private val trader              =
    TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(true))
  private val goodsDetails        = GoodsDetails("name", "description", None, None, None, None, None)
  private val submissionReference = "submissionReference"
  private val method              = MethodOne(None, None, None)
  private val contact             = ContactDetails("name", "email", None, None, None)
  private val now                 = Instant.now.truncatedTo(ChronoUnit.MILLIS)

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
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = None,
        submissionReference = "submissionReference",
        created = now,
        lastUpdated = now
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
        submissionReference = "submissionReference",
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = None,
        created = now,
        lastUpdated = now
      )

      repository.set(application).futureValue
      repository.set(application).failed.futureValue
    }
  }

  ".get" - {

    "must return an application when one exists for this eori" in {

      val application = Application(
        id = ApplicationId(1),
        applicantEori = "applicantEori",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = None,
        submissionReference = "submissionReference",
        created = now,
        lastUpdated = now
      )

      insert(application).futureValue
      val result = repository.get(ApplicationId(1), "applicantEori").futureValue
      result.value mustEqual application
    }

    "must return None when an application exists but with a different eori" in {

      val application = Application(
        id = ApplicationId(1),
        applicantEori = "applicantEori",
        trader = trader,
        agent = None,
        contact = contact,
        goodsDetails = goodsDetails,
        requestedMethod = method,
        attachments = Nil,
        whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
        letterOfAuthority = None,
        submissionReference = "submissionReference",
        created = now,
        lastUpdated = now
      )

      insert(application).futureValue
      val result = repository.get(ApplicationId(1), "otherEori").futureValue
      result must not be defined
    }

    "must return None when an application does not exist" in {

      val result = repository.get(ApplicationId(1), "applicantEori").futureValue
      result must not be defined
    }
  }

  ".summaries" - {

    "must return summaries of all applications for the given eori" in {

      val eori1        = "eori 1"
      val eori2        = "eori2"
      val application1 = Application(
        ApplicationId(1),
        eori1,
        trader,
        None,
        contact,
        method,
        goodsDetails,
        Nil,
        Some(WhatIsYourRole.EmployeeOrg),
        None,
        submissionReference,
        now,
        now
      )
      val application2 = Application(
        ApplicationId(2),
        eori1,
        trader,
        None,
        contact,
        method,
        goodsDetails,
        Nil,
        Some(WhatIsYourRole.EmployeeOrg),
        None,
        submissionReference,
        now,
        now
      )
      val application3 = Application(
        ApplicationId(3),
        eori2,
        trader,
        None,
        contact,
        method,
        goodsDetails,
        Nil,
        Some(WhatIsYourRole.EmployeeOrg),
        None,
        submissionReference,
        now,
        now
      )

      insert(application1).futureValue
      insert(application2).futureValue
      insert(application3).futureValue

      val result = repository.summaries(eori1).futureValue

      result must contain theSameElementsAs Seq(
        ApplicationSummary(ApplicationId(1), "name", now, "eori"),
        ApplicationSummary(ApplicationId(2), "name", now, "eori")
      )
    }
  }
}
