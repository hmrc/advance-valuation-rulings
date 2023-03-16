package uk.gov.hmrc.advancevaluationrulings.repositories

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.advancevaluationrulings.utils.BaseIntegrationSpec
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class ValuationRulingsRepositorySpec
    extends BaseIntegrationSpec
    with DefaultPlayMongoRepositorySupport[ValuationRulingsApplication] {

  override val checkTtlIndex: Boolean = false

  override protected val repository = new ValuationRulingsRepositoryImpl(
    mongoComponent
  )

  override def beforeEach(): Unit = {
    deleteAll().futureValue
    prepareDatabase()
  }

  "ValuationRulingsRepository" should {
    "insert and retrieve single application" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        application =>
          repository.insert(application).futureValue mustBe true

          repository
            .getItems(application.data.checkRegisteredDetails.eori)
            .futureValue mustBe Seq(application)
      }
    }

    "insert and retrieve multiple applications in descending order of the applicationNumber" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        application =>
          val emptySpace              = " " * 3
          val secondApplicationNumber = emptySpace + application.applicationNumber
          val secondApplication       = application.copy(applicationNumber = secondApplicationNumber)

          repository.insert(application).futureValue mustBe true
          repository.insert(secondApplication).futureValue mustBe true

          repository
            .getItems(application.data.checkRegisteredDetails.eori)
            .futureValue mustBe Seq(application, secondApplication)
            .sortBy(_.applicationNumber)
            .reverse
      }
    }

    "not allow an application to be inserted more than once" in {
      ScalaCheckPropertyChecks.forAll(valuationRulingsApplicationGen) {
        application =>
          repository.insert(application).futureValue mustBe true

          repository
            .getItems(application.data.checkRegisteredDetails.eori)
            .futureValue mustBe Seq(application)

          val ex = the[Exception] thrownBy repository.insert(application).futureValue
          ex.getMessage must include("duplicate key error")
      }
    }
  }
}
