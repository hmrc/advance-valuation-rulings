package uk.gov.hmrc.advancevaluationrulings.repositories

import generators.{CaseManagementGenerators, ModelGenerators}
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.advancevaluationrulings.models._
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class ValuationCaseRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[ValuationCase]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with ModelGenerators
    with CaseManagementGenerators {

  override val checkTtlIndex: Boolean = false

  override protected val repository = new ValuationCaseRepository(mongoComponent)

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  ".create" - {
    "must save the item, with the status as New" in {
      val valuationApplication = valuationApplicationGen.sample.get
      val expectedValuationCase = ValuationCase(
        "referenceValue",
        CaseStatus.NEW,
        instant,
        1,
        valuationApplication,
        1,
        None,
        None,
        None,
        List.empty,
        Set.empty,
        None,
        None
      )

      repository.create(expectedValuationCase).futureValue

      val insertedValuationCase = find(Filters.equal("reference", expectedValuationCase.reference)).futureValue.head

      insertedValuationCase mustEqual expectedValuationCase
      insertedValuationCase.status mustEqual CaseStatus.NEW
    }

    "fails to create a case if there is an item with the same reference" in {
      val valuationApplication = valuationApplicationGen.sample.get
      val expectedValuationCase = ValuationCase(
        "referenceValue",
        CaseStatus.NEW,
        instant,
        1,
        valuationApplication,
        1,
        None,
        None,
        None,
        List.empty,
        Set.empty,
        None,
        None
      )

      repository.create(expectedValuationCase).futureValue

      val exception = intercept[Exception] {
         repository.create(expectedValuationCase).futureValue
      }

      exception.getMessage.contains("11000") mustEqual true
    }
  }

  ".assign" - {
    "must change the status of the item to Open and updates the case worker" in {
      val valuationApplication = valuationApplicationGen.sample.get
      val valuationCase = ValuationCase(
        "referenceValue",
        CaseStatus.NEW,
        instant,
        1,
        valuationApplication,
        1,
        None,
        None,
        None,
        List.empty,
        Set.empty,
        None,
        None
      )

      val caseWorker = caseWorkerGen.sample.get

      repository.create(valuationCase).futureValue
      repository.assignCase(valuationCase.reference, caseWorker)

      val insertedValuationCase = find(Filters.equal("reference", valuationCase.reference)).futureValue.head

      insertedValuationCase.status mustEqual CaseStatus.OPEN
      insertedValuationCase.assignee.value mustEqual caseWorker
    }
  }
}
