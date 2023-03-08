package uk.gov.hmrc.advancevaluationrulings.repository

import uk.gov.hmrc.advancevaluationrulings.models.ValuationRulingsApplication
import uk.gov.hmrc.advancevaluationrulings.models.common._
import uk.gov.hmrc.advancevaluationrulings.repositories.ValuationRulingsRepositoryImpl
import uk.gov.hmrc.advancevaluationrulings.utils.BaseIntegrationSpec
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport

import java.time.Instant
import java.time.temporal.ChronoUnit

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

  private val testApplication = ValuationRulingsApplication(
    id = "testId",
    data = UserAnswers(
      importGoods = true,
      checkRegisteredDetails = RegisteredDetailsCheck(
        value = true,
        eori = "testEori",
        name = "test name",
        streetAndNumber = "test street and num",
        city = "test city",
        country = "United Kingdom",
        postalCode = "N123 ACS"
      ),
      applicationContactDetails = ApplicationContactDetails(
        name = "test contact name",
        email = "some@email.com",
        phone = "12346444"
      ),
      valuationMethod = ValuationMethod.Method1,
      isThereASaleInvolved = true,
      isSaleBetweenRelatedParties = true,
      areThereRestrictionsOnTheGoods = true,
      isTheSaleSubjectToConditions = true,
      descriptionOfGoods = "socks",
      hasCommodityCode = true,
      commodityCode = "12344",
      haveTheGoodsBeenSubjectToLegalChallenges = true,
      hasConfidentialInformation = true,
      doYouWantToUploadDocuments = true
    ),
    applicationNumber = "some-app-number",
    lastUpdated = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  )

  "ValuationRulingsRepository" should {
    "insert and retrieve application" in {
      repository.insert(testApplication).futureValue

      repository.collection.countDocuments().toFuture().futureValue mustBe 1

      repository.getItem(testApplication.id).futureValue.value mustBe testApplication
    }
  }

}
