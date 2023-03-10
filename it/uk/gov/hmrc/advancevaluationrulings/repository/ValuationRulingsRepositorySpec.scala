package uk.gov.hmrc.advancevaluationrulings.repository

import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.advancevaluationrulings.repositories.ValuationRulingsRepositoryImpl
import uk.gov.hmrc.advancevaluationrulings.utils.BaseIntegrationSpec
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ValuationRulingsRepositorySpec
    extends BaseIntegrationSpec
    with PlayMongoRepositorySupport[ValuationRulingsApplication] {

  override protected def repository = new ValuationRulingsRepositoryImpl(
    mongoComponent
  )

  override def beforeEach(): Unit = {
    deleteAll().futureValue
    prepareDatabase()
  }

  "ValuationRulingsRepository" should {
    "insert and retrieve application" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        application =>
          repository.insert(application).futureValue mustBe true

          repository.getItem(application.id).futureValue.value mustBe application
      }
    }

    "not allow an application to be inserted more than once" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        application =>
          repository.insert(application).futureValue mustBe true

          repository.getItem(application.id).futureValue.value mustBe application

          val ex = the[Exception] thrownBy repository.insert(application).futureValue
          ex.getMessage must include("duplicate key error")
      }
    }
  }

}
