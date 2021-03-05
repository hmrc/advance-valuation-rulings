/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bindingtariffclassification.repository

import java.time._

import cats.syntax.all._
import org.mockito.BDDMockito.given
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.indexes.Index
import reactivemongo.api.indexes.IndexType.Ascending
import reactivemongo.api.{Cursor, DB, ReadConcern}
import reactivemongo.bson._
import reactivemongo.core.errors.DatabaseException
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters.formatCase
import uk.gov.hmrc.bindingtariffclassification.model.reporting._
import uk.gov.hmrc.bindingtariffclassification.model.{CaseStatus, PseudoCaseStatus, _}
import uk.gov.hmrc.bindingtariffclassification.sort.{CaseSortField, SortDirection}
import uk.gov.hmrc.mongo.MongoSpecSupport
import util.CaseData._
import util.Cases._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CaseRepositorySpec
    extends BaseMongoIndexSpec
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with MongoSpecSupport
    with Eventually {
  self =>

  private val conflict = 11000

  private val mongoDbProvider: MongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val config     = mock[AppConfig]
  private val repository = newMongoRepository

  private def newMongoRepository: CaseMongoRepository =
    new CaseMongoRepository(config, mongoDbProvider, new SearchMapper(config), new UpdateMapper)

  private val case1: Case     = createCase()
  private val case2: Case     = createCase()
  private val liabCase1: Case = createCase(app = createLiabilityOrder)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repository.drop)
  }

  private def collectionSize: Int =
    await(
      repository.collection
        .count(selector = None, limit = Some(0), skip = 0, hint = None, readConcern = ReadConcern.Local)
    ).toInt

  "deleteAll" should {

    "clear the collection" in {
      val size = collectionSize

      store(case1, case2)
      collectionSize shouldBe 2 + size

      await(repository.deleteAll()) shouldBe ((): Unit)
      collectionSize                shouldBe size
    }

  }

  "delete" should {

    "remove the matching case" in {
      val c1 = createCase(r = "REF_1")
      val c2 = createCase(r = "REF_2")

      val size = collectionSize

      store(c1, c2)
      collectionSize shouldBe 2 + size

      await(repository.delete("REF_1")) shouldBe ((): Unit)
      collectionSize                    shouldBe 1 + size

      await(repository.collection.find(selectorByReference(c1)).one[Case]) shouldBe None
      await(repository.collection.find(selectorByReference(c2)).one[Case]) shouldBe Some(c2)
    }

  }

  "insert" should {

    "insert a new document in the collection" in {
      val size = collectionSize

      await(repository.insert(case1))                                         shouldBe case1
      collectionSize                                                          shouldBe 1 + size
      await(repository.collection.find(selectorByReference(case1)).one[Case]) shouldBe Some(case1)
    }

    "fail to insert an existing document in the collection" in {
      await(repository.insert(case1)) shouldBe case1
      val size = collectionSize

      val caught = intercept[DatabaseException] {
        await(repository.insert(case1))
      }
      caught.code shouldBe Some(conflict)

      collectionSize shouldBe size
    }

  }

  "update" should {

    "modify an existing document in the collection" in {
      await(repository.insert(case1)) shouldBe case1
      val size = collectionSize

      val updated: Case = case1.copy(application = createBasicBTIApplication, status = CaseStatus.CANCELLED)
      await(repository.update(updated, upsert = false)) shouldBe Some(updated)
      collectionSize shouldBe size

      await(repository.collection.find(selectorByReference(updated)).one[Case]) shouldBe Some(updated)
    }

    "do nothing when trying to update an unknown document" in {
      val size = collectionSize

      await(repository.update(case1, upsert = false)) shouldBe None
      collectionSize shouldBe size
    }

    "upsert a new existing document in the collection" in {
      val size = collectionSize

      await(repository.update(case1, upsert = true)) shouldBe Some(case1)
      collectionSize shouldBe size + 1
    }
  }

  "update with CaseUpdate" should {
    val now = LocalDateTime.of(2021, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)

    "modify an ATaR case in the collection" in {

      await(repository.insert(case1)) shouldBe case1
      val size = collectionSize

      val applicationPdf = Some(Attachment("id", true, None, now, None, false))
      val atarCaseUpdate = CaseUpdate(application = Some(BTIUpdate(applicationPdf = SetValue(applicationPdf))))
      val updated: Case =
        case1.copy(application = case1.application.asInstanceOf[BTIApplication].copy(applicationPdf = applicationPdf))

      await(repository.update(case1.reference, atarCaseUpdate)) shouldBe Some(updated)
      collectionSize                                            shouldBe size

      await(repository.collection.find(selectorByReference(updated)).one[Case]) shouldBe Some(updated)
    }

    "modify a liability case in the collection" in {
      await(repository.insert(liabCase1)) shouldBe liabCase1
      val size = collectionSize

      val liabCaseUpdate = CaseUpdate(application = Some(LiabilityUpdate(traderName = SetValue("foo"))))
      val updated: Case =
        liabCase1.copy(application = liabCase1.application.asInstanceOf[LiabilityOrder].copy(traderName = "foo"))

      await(repository.update(liabCase1.reference, liabCaseUpdate)) shouldBe Some(updated)
      collectionSize                                                shouldBe size

      await(repository.collection.find(selectorByReference(updated)).one[Case]) shouldBe Some(updated)
    }

    "do nothing when trying to update an unknown document" in {
      val size           = collectionSize
      val liabCaseUpdate = CaseUpdate(application = Some(LiabilityUpdate(traderName = SetValue("foo"))))
      await(repository.update(liabCase1.reference, liabCaseUpdate)) shouldBe None
      collectionSize                                                shouldBe size
    }
  }

  "get without search parameters" should {

    "retrieve all cases from the collection, sorted by insertion order" in {
      val search = CaseSearch()

      await(repository.insert(case1))
      await(repository.insert(case2))
      collectionSize shouldBe 2

      await(repository.get(search, Pagination())).results shouldBe Seq(case1, case2)
    }

    "return all cases from the collection sorted in ascending order" in {
      val search = CaseSearch(sort = Some(CaseSort(Set(CaseSortField.DAYS_ELAPSED), SortDirection.ASCENDING)))

      val oldCase = case1.copy(daysElapsed = 1)
      val newCase = case2.copy(daysElapsed = 0)
      await(repository.insert(oldCase))
      await(repository.insert(newCase))

      collectionSize shouldBe 2

      await(repository.get(search, Pagination())).results shouldBe Seq(newCase, oldCase)
    }

    "return all cases from the collection sorted in complex order, when specified" in {
      val search = CaseSearch(sort =
        Some(
          CaseSort(
            Set(CaseSortField.APPLICATION_TYPE, CaseSortField.APPLICATION_STATUS, CaseSortField.DAYS_ELAPSED),
            SortDirection.DESCENDING
          )
        )
      )

      val oldCase  = case1.copy(daysElapsed = 1)
      val newCase  = case2.copy(daysElapsed = 0)
      val liabCase = liabCase1
      await(repository.insert(oldCase))
      await(repository.insert(newCase))
      await(repository.insert(liabCase))

      collectionSize shouldBe 3

      await(repository.get(search, Pagination())).results shouldBe Seq(liabCase, oldCase, newCase)
    }

    "return all cases from the collection sorted in descending order" in {

      val search = CaseSearch(sort = Some(CaseSort(Set(CaseSortField.DAYS_ELAPSED), SortDirection.DESCENDING)))

      val oldCase = case1.copy(daysElapsed = 1)
      val newCase = case2.copy(daysElapsed = 0)
      await(repository.insert(oldCase))
      await(repository.insert(newCase))

      collectionSize shouldBe 2

      await(repository.get(search, Pagination())).results shouldBe Seq(oldCase, newCase)
    }

    "return an empty sequence when there are no cases in the collection" in {
      val search = CaseSearch()
      await(repository.get(search, Pagination())).results shouldBe Seq.empty[Case]
    }
  }

  "get by queueId" should {

    val queueIdX       = Some("queue_x")
    val queueIdY       = Some("queue_y")
    val unknownQueueId = Some("unknown_queue_id")

    val caseWithEmptyQueue = createCase()
    val caseWithQueueX1    = createCase().copy(queueId = queueIdX)
    val caseWithQueueX2    = createCase().copy(queueId = queueIdX)
    val caseWithQueueY     = createCase().copy(queueId = queueIdY)

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(queueId = unknownQueueId.map(Set(_))))

      store(caseWithEmptyQueue, caseWithQueueX1)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      val search = CaseSearch(CaseFilter(queueId = queueIdX.map(Set(_))))
      store(caseWithEmptyQueue, caseWithQueueX1, caseWithQueueY)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithQueueX1)
    }

    "return the expected documents when there are multiple matches" in {
      val search = CaseSearch(CaseFilter(queueId = queueIdX.map(Set(_))))

      store(caseWithEmptyQueue, caseWithQueueX1, caseWithQueueX2, caseWithQueueY)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithQueueX1, caseWithQueueX2)
    }

  }

  "get by minDecisionDate" should {

    val futureDate = LocalDate.of(3000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    val pastDate   = LocalDate.of(1970, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)

    val decisionExpired         = createDecision(effectiveEndDate = Some(pastDate))
    val decisionFuture          = createDecision(effectiveEndDate = Some(futureDate))
    val caseWithExpiredDecision = createCase(decision             = Some(decisionExpired))
    val caseWithFutureDecision  = createCase(decision             = Some(decisionFuture))

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(minDecisionEnd = Some(Instant.now())))
      store(caseWithExpiredDecision)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is a match" in {
      val search = CaseSearch(CaseFilter(minDecisionEnd = Some(Instant.now())))
      store(caseWithExpiredDecision, caseWithFutureDecision)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithFutureDecision)
    }

  }

  "get by assigneeId" should {

    val assigneeX       = Operator("assignee_x")
    val assigneeY       = Operator("assignee_y")
    val unknownAssignee = Operator("unknown_assignee_id")

    val caseWithEmptyAssignee = createCase()
    val caseWithAssigneeX1    = createCase().copy(assignee = Some(assigneeX))
    val caseWithAssigneeX2    = createCase().copy(assignee = Some(assigneeX))
    val caseWithAssigneeY1    = createCase().copy(assignee = Some(assigneeY))

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(assigneeId = Some(unknownAssignee.id)))
      store(caseWithEmptyAssignee, caseWithAssigneeX1)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      val search = CaseSearch(CaseFilter(assigneeId = Some(assigneeX.id)))
      store(caseWithEmptyAssignee, caseWithAssigneeX1, caseWithAssigneeY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithAssigneeX1)
    }

    "return the expected documents - with 'none'" in {
      val search = CaseSearch(CaseFilter(assigneeId = Some("none")))
      store(caseWithEmptyAssignee, caseWithAssigneeX1, caseWithAssigneeY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithEmptyAssignee)
    }

    "return the expected documents - with 'some'" in {
      val search = CaseSearch(CaseFilter(assigneeId = Some("some")))
      store(caseWithEmptyAssignee, caseWithAssigneeX1, caseWithAssigneeY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithAssigneeX1, caseWithAssigneeY1)
    }

    "return the expected documents when there are multiple matches" in {
      val search = CaseSearch(CaseFilter(assigneeId = Some(assigneeX.id)))
      store(caseWithEmptyAssignee, caseWithAssigneeX1, caseWithAssigneeX2, caseWithAssigneeY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithAssigneeX1, caseWithAssigneeX2)
    }

  }

  "get by concrete status" should {

    val caseWithStatusX1 = createCase().copy(status = CaseStatus.NEW)
    val caseWithStatusX2 = createCase().copy(status = CaseStatus.NEW)
    val caseWithStatusY1 = createCase().copy(status = CaseStatus.OPEN)

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.DRAFT))))
      store(caseWithStatusX1)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.NEW))))
      store(caseWithStatusX1, caseWithStatusY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithStatusX1)
    }

    "return the expected documents when there are multiple matches" in {
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.NEW))))
      store(caseWithStatusX1, caseWithStatusX2, caseWithStatusY1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithStatusX1, caseWithStatusX2)
    }

  }

  "get by pseudo status" should {
    val now = LocalDateTime.of(2019, 1, 1, 0, 0).toInstant(ZoneOffset.UTC)

    val newCase = createCase(r = "new", status = CaseStatus.NEW)
    val liveCase = createCase(
      r        = "live",
      status   = CaseStatus.COMPLETED,
      decision = Some(createDecision(effectiveEndDate = Some(now.plusSeconds(1))))
    )
    val expiredCase = createCase(
      r        = "expired",
      status   = CaseStatus.COMPLETED,
      decision = Some(createDecision(effectiveEndDate = Some(now.minusSeconds(1))))
    )

    "return an empty sequence when there are no matches" in {
      given(config.clock) willReturn Clock.fixed(now, ZoneOffset.UTC)

      store(newCase)
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.LIVE))))
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      given(config.clock) willReturn Clock.fixed(now, ZoneOffset.UTC)

      store(newCase, liveCase, expiredCase)
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.LIVE))))
      await(repository.get(search, Pagination())).results shouldBe Seq(liveCase)
    }

    "return the expected documents when there are multiple matches" in {
      given(config.clock) willReturn Clock.fixed(now, ZoneOffset.UTC)

      store(newCase, liveCase, expiredCase)
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.LIVE, PseudoCaseStatus.EXPIRED))))
      await(repository.get(search, Pagination())).results shouldBe Seq(expiredCase, liveCase)
    }

  }

  "get by multiple statuses" should {

    val caseWithStatusX1 = createCase().copy(status = CaseStatus.NEW)
    val caseWithStatusX2 = createCase().copy(status = CaseStatus.NEW)
    val caseWithStatusY1 = createCase().copy(status = CaseStatus.OPEN)
    val caseWithStatusZ1 = createCase().copy(status = CaseStatus.CANCELLED)
    val caseWithStatusW1 = createCase().copy(status = CaseStatus.SUPPRESSED)

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(statuses = Some(Set(PseudoCaseStatus.DRAFT, PseudoCaseStatus.REFERRED))))
      store(caseWithStatusX1, caseWithStatusX2, caseWithStatusY1, caseWithStatusZ1, caseWithStatusW1)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      val search = CaseSearch(
        CaseFilter(statuses = Some(Set(PseudoCaseStatus.NEW, PseudoCaseStatus.REFERRED, PseudoCaseStatus.SUSPENDED)))
      )
      store(caseWithStatusX1, caseWithStatusY1, caseWithStatusZ1, caseWithStatusW1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithStatusX1)
    }

    "return the expected documents when there are multiple matches" in {
      val search = CaseSearch(
        CaseFilter(statuses = Some(Set(PseudoCaseStatus.NEW, PseudoCaseStatus.DRAFT, PseudoCaseStatus.OPEN)))
      )
      store(caseWithStatusX1, caseWithStatusX2, caseWithStatusY1, caseWithStatusZ1, caseWithStatusW1)
      await(repository.get(search, Pagination())).results shouldBe Seq(
        caseWithStatusX1,
        caseWithStatusX2,
        caseWithStatusY1
      )
    }

  }

  "get by multiple references" should {
    val caseWithReferenceX1 = createCase().copy(reference = "x1")
    val caseWithReferenceX2 = createCase().copy(reference = "x2")
    val caseWithReferenceY1 = createCase().copy(reference = "y1")
    val caseWithReferenceZ1 = createCase().copy(reference = "z1")
    val caseWithReferenceW1 = createCase().copy(reference = "w1")

    "return an empty sequence when there are no matches" in {
      val search = CaseSearch(CaseFilter(reference = Some(Set("a", "b"))))
      store(caseWithReferenceX1, caseWithReferenceX2, caseWithReferenceY1, caseWithReferenceZ1, caseWithReferenceW1)
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      val search = CaseSearch(CaseFilter(reference = Some(Set("x1"))))
      store(caseWithReferenceX1, caseWithReferenceY1, caseWithReferenceZ1, caseWithReferenceW1)
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithReferenceX1)
    }

    "return the expected documents when there are multiple matches" in {
      val search = CaseSearch(CaseFilter(reference = Some(Set("x1", "x2", "y1"))))
      store(caseWithReferenceX1, caseWithReferenceX2, caseWithReferenceY1, caseWithReferenceZ1, caseWithReferenceW1)
      await(repository.get(search, Pagination())).results shouldBe Seq(
        caseWithReferenceX1,
        caseWithReferenceX2,
        caseWithReferenceY1
      )
    }

  }

  "get by single keyword" should {

    val c1 = createCase(keywords = Set("BIKE", "MTB"))
    val c2 = createCase(keywords = Set("KNIFE", "KITCHEN"))
    val c3 = createCase(keywords = Set("BIKE", "HARDTAIL"))

    "return an empty sequence when there are no matches" in {
      store(case1, c1)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("KNIFE"))))
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(case1, c1, c2)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("KNIFE"))))
      await(repository.get(search, Pagination())).results shouldBe Seq(c2)
    }

    "return the expected documents when there are multiple matches" in {
      store(case1, c1, c2, c3)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("BIKE"))))
      await(repository.get(search, Pagination())).results shouldBe Seq(c1, c3)
    }

  }

  "get by multiple keywords" should {

    val c1 = createCase(keywords = Set("BIKE", "MTB"))
    val c2 = createCase(keywords = Set("BIKE", "MTB", "29ER"))
    val c3 = createCase(keywords = Set("BIKE", "HARDTAIL"))

    "return an empty sequence when there are no matches" in {
      store(case1, c1, c2, c3)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("BIKE", "MTB", "HARDTAIL"))))
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(case1, c1, c2, c3)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("BIKE", "MTB", "29ER"))))
      await(repository.get(search, Pagination())).results shouldBe Seq(c2)
    }

    "return the expected documents when there are multiple matches" in {
      store(case1, c1, c2, c3)
      val search = CaseSearch(CaseFilter(keywords = Some(Set("BIKE", "MTB"))))
      await(repository.get(search, Pagination())).results shouldBe Seq(c1, c2)
    }

  }

  "get by trader name" should {

    val novakApp = createBasicBTIApplication.copy(holder = createEORIDetails.copy(businessName = "Novak Djokovic"))
    val caseX    = createCase(app                        = novakApp)

    "return an empty sequence when there are no matches" in {
      store(case1, caseX)

      await(repository.get(CaseSearch(CaseFilter(traderName = Some("Alfred"))), Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(case1, caseX)

      // full name search
      await(repository.get(CaseSearch(CaseFilter(traderName = Some("Novak Djokovic"))), Pagination())).results shouldBe Seq(
        caseX
      )

      // substring search
      await(repository.get(CaseSearch(CaseFilter(traderName = Some("Novak"))), Pagination())).results shouldBe Seq(
        caseX
      )
      await(repository.get(CaseSearch(CaseFilter(traderName = Some("Djokovic"))), Pagination())).results shouldBe Seq(
        caseX
      )

      // case-insensitive
      await(repository.get(CaseSearch(CaseFilter(traderName = Some("novak djokovic"))), Pagination())).results shouldBe Seq(
        caseX
      )
    }

    "return the expected documents when there are multiple matches" in {
      val novakApp2 = createBasicBTIApplication.copy(holder = createEORIDetails.copy(businessName = "Novak Djokovic 2"))
      val caseX2    = createCase(app                        = novakApp2)
      store(caseX, caseX2)

      val search = CaseSearch(CaseFilter(traderName = Some("Novak Djokovic")))
      await(repository.get(search, Pagination())).results shouldBe Seq(caseX, caseX2)
    }
  }

  "get by eori" should {

    val holderEori = "01234"
    val agentEori  = "98765"

    val agentDetails = createAgentDetails().copy(eoriDetails = createEORIDetails.copy(eori = agentEori))

    val holderApp = createBasicBTIApplication.copy(holder = createEORIDetails.copy(eori = holderEori), agent = None)
    val agentApp = createBTIApplicationWithAllFields()
      .copy(holder = createEORIDetails.copy(eori = holderEori), agent = Some(agentDetails))

    val agentCase  = createCase(app = agentApp)
    val holderCase = createCase(app = holderApp)

    "return an empty sequence when there are no matches" in {
      store(agentCase, holderCase)

      await(repository.get(CaseSearch(CaseFilter(eori = Some("333"))), Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(agentCase, holderCase)

      await(repository.get(CaseSearch(CaseFilter(eori = Some("98765"))), Pagination())).results shouldBe Seq(agentCase)
    }

    "return the expected documents when there are multiple matches" in {
      store(agentCase, holderCase)

      await(repository.get(CaseSearch(CaseFilter(eori = Some("01234"))), Pagination())).results shouldBe Seq(
        agentCase,
        holderCase
      )
    }

  }

  "get by decision details" should {

    val c1 = createCase(decision = Some(createDecision(goodsDescription             = "Amazing HTC smartphone")))
    val c2 = createCase(decision = Some(createDecision(methodCommercialDenomination = Some("amazing football shoes"))))
    val c3 = createCase(decision = Some(createDecision(justification                = "this is absolutely AAAAMAZINGGGG")))

    "return an empty sequence when there are no matches" in {
      store(case1, c1, c2, c3)
      await(repository.get(CaseSearch(CaseFilter(decisionDetails = Some("table"))), Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(case1, c1, c2, c3)
      await(repository.get(CaseSearch(CaseFilter(decisionDetails = Some("Football"))), Pagination())).results shouldBe Seq(
        c2
      )
    }

    "return the expected documents when there are multiple matches" in {
      store(case1, c1, c2, c3)
      await(repository.get(CaseSearch(CaseFilter(decisionDetails = Some("amazing"))), Pagination())).results shouldBe Seq(
        c1,
        c2,
        c3
      )
    }
  }

  "get by commodity code name" should {

    val caseX = createNewCaseWithExtraFields()
    val caseY = createNewCaseWithExtraFields().copy(reference = "88888888")

    "return an empty sequence when there are no matches" in {
      store(case1)
      val search = CaseSearch(CaseFilter(commodityCode = Some("234")))
      await(repository.get(search, Pagination())).results shouldBe Seq.empty
    }

    "return the expected document when there is one match" in {
      store(caseX, case1)
      val search = CaseSearch(CaseFilter(commodityCode = Some("12345")))
      await(repository.get(search, Pagination())).results shouldBe Seq(caseX)
    }

    "return the expected documents when there are multiple matches" in {
      store(case1, caseX, caseY)
      val search = CaseSearch(CaseFilter(commodityCode = Some("12345")))
      await(repository.get(search, Pagination())).results shouldBe Seq(caseX, caseY)
    }
  }

  "get by type " should {

    "filter on only BTI when specified in search" in {
      store(case1, case2, liabCase1)
      val search = CaseSearch(CaseFilter(applicationType = Some(Set(ApplicationType.BTI))))
      await(repository.get(search, Pagination())).results shouldBe Seq(case1, case2)
    }

    "filter on only Liability when specified in search" in {
      store(case1, case2, liabCase1)
      val search = CaseSearch(CaseFilter(applicationType = Some(Set(ApplicationType.LIABILITY_ORDER))))
      await(repository.get(search, Pagination())).results shouldBe Seq(liabCase1)
    }

    "not filter on type when both specified in search" in {
      store(case1, case2, liabCase1)
      val search =
        CaseSearch(CaseFilter(applicationType = Some(Set(ApplicationType.BTI, ApplicationType.LIABILITY_ORDER))))
      await(repository.get(search, Pagination())).results shouldBe Seq(case1, case2, liabCase1)
    }
  }

  "get by migration status" should {
    val case1Migrated     = case1.copy(dateOfExtract     = Some(Instant.now()))
    val liabCase1Migrated = liabCase1.copy(dateOfExtract = Some(Instant.now()))

    "return only migrated cases when specified in search" in {
      store(case1Migrated, case2, liabCase1Migrated)
      val search = CaseSearch(CaseFilter(migrated = Some(true)))
      await(repository.get(search, Pagination())).results shouldBe Seq(case1Migrated, liabCase1Migrated)
    }
    "return only cases that were not migrated when specified in search" in {
      store(case1Migrated, case2, liabCase1Migrated)
      val search = CaseSearch(CaseFilter(migrated = Some(false)))
      await(repository.get(search, Pagination())).results shouldBe Seq(case2)
    }
    "return all cases when there is no migrated filter" in {
      store(case1Migrated, case2, liabCase1Migrated)
      val search = CaseSearch(CaseFilter())
      await(repository.get(search, Pagination())).results shouldBe Seq(case1Migrated, case2, liabCase1Migrated)
    }
  }

  "pagination" should {

    "return some cases with default Pagination" in {
      store(case1)
      store(case2)
      await(repository.get(CaseSearch(), Pagination())).size shouldBe 2
    }

    "return upto 'pageSize' cases" in {
      store(case1)
      store(case2)
      await(repository.get(CaseSearch(), Pagination(pageSize = 1))).size shouldBe 1
    }

    "return pages of cases" in {
      store(case1)
      store(case2)
      await(repository.get(CaseSearch(), Pagination(pageSize = 1))).size shouldBe 1
      await(repository.get(CaseSearch(), Pagination(page     = 2, pageSize = 1))).size shouldBe 1
      await(repository.get(CaseSearch(), Pagination(page     = 3, pageSize = 1))).size shouldBe 0
    }
  }

  "get by queueId, assigneeId and status" should {

    val assigneeX = Operator("assignee_x")
    val assigneeY = Operator("assignee_y")
    val queueIdX  = Some("queue_x")
    val queueIdY  = Some("queue_y")
    val statusX   = CaseStatus.NEW
    val statusY   = CaseStatus.OPEN

    val caseWithNoQueueAndNoAssignee = createCase()
    val caseWithQxAndAxAndSx         = createCase().copy(queueId = queueIdX, assignee = Some(assigneeX), status = statusX)
    val caseWithQxAndAxAndSy         = createCase().copy(queueId = queueIdX, assignee = Some(assigneeX), status = statusY)
    val caseWithQxAndAyAndSx         = createCase().copy(queueId = queueIdX, assignee = Some(assigneeY), status = statusX)
    val caseWithQxAndAyAndSy         = createCase().copy(queueId = queueIdX, assignee = Some(assigneeY), status = statusY)
    val caseWithQyAndAxAndSx         = createCase().copy(queueId = queueIdY, assignee = Some(assigneeX), status = statusX)
    val caseWithQyAndAxAndSy         = createCase().copy(queueId = queueIdY, assignee = Some(assigneeX), status = statusY)

    "filter as expected" in {
      val search = CaseSearch(
        CaseFilter(
          queueId    = queueIdX.map(Set(_)),
          assigneeId = Some(assigneeX.id),
          statuses   = Some(Set(PseudoCaseStatus.NEW))
        )
      )

      store(
        caseWithNoQueueAndNoAssignee,
        caseWithQxAndAxAndSx,
        caseWithQxAndAxAndSy,
        caseWithQxAndAyAndSx,
        caseWithQxAndAyAndSy,
        caseWithQyAndAxAndSx,
        caseWithQyAndAxAndSy
      )
      await(repository.get(search, Pagination())).results shouldBe Seq(caseWithQxAndAxAndSx)
    }

  }

  "get by reference" should {

    "retrieve the correct document" in {
      await(repository.insert(case1))
      collectionSize shouldBe 1

      await(repository.getByReference(case1.reference)) shouldBe Some(case1)
    }

    "return 'None' when the 'reference' doesn't match any document in the collection" in {
      for (_ <- 1 to 3) {
        await(repository.insert(createCase()))
      }
      collectionSize shouldBe 3

      await(repository.getByReference("WRONG_REFERENCE")) shouldBe None
    }
  }

  "SummaryReport" should {
    val c1 = aCase(
      withQueue("1"),
      withActiveDaysElapsed(2),
      withReferredDaysElapsed(1),
      withReference("1"),
      withStatus(CaseStatus.OPEN),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9506999000")
    )
    val c2 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(2),
      withReference("2"),
      withStatus(CaseStatus.OPEN),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9507900000")
    )
    val c3 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(7),
      withReference("3"),
      withStatus(CaseStatus.NEW),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withDecision("8518300090")
    )
    val c4 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(7),
      withReferredDaysElapsed(6),
      withReference("4"),
      withStatus(CaseStatus.NEW),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withoutDecision()
    )
    val c5 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("5"),
      withStatus(CaseStatus.REFERRED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val c6 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(5),
      withReferredDaysElapsed(0),
      withReference("6"),
      withStatus(CaseStatus.REFERRED),
      withAssignee(Some(Operator("3"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withoutDecision()
    )
    val cases = List(c1, c2, c3, c4, c5, c6)

    val c7 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("7"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val c8 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(8),
      withReferredDaysElapsed(0),
      withReference("8"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("3"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val liveCases = List(c7, c8)

    val c9 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("9"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision(
        "9507209000",
        effectiveStartDate = Some(Instant.parse("2018-01-31T09:00:00.00Z")),
        effectiveEndDate   = Some(Instant.parse("2021-01-31T09:00:00.00Z"))
      )
    )
    val c10 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(9),
      withReferredDaysElapsed(0),
      withReference("10"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("3"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision(
        "9507209000",
        effectiveStartDate = Some(Instant.parse("2018-01-31T09:00:00.00Z")),
        effectiveEndDate   = Some(Instant.parse("2021-01-31T09:00:00.00Z"))
      )
    )
    val expiredCases = List(c9, c10)
    val c11 = aCase(
      withActiveDaysElapsed(2),
      withReferredDaysElapsed(0),
      withReference("11"),
      withStatus(CaseStatus.NEW),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z"))
    )
    val c12 = aCase(
      withActiveDaysElapsed(1),
      withReferredDaysElapsed(0),
      withReference("12"),
      withStatus(CaseStatus.NEW),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z"))
    )
    val gatewayCases = List(c11, c12)

    val c13 = aCase(liabCase1.copy(application = liabCase1.application.asLiabilityOrder.copy(status = LiabilityStatus.NON_LIVE)))(
      withQueue("3"),
      withActiveDaysElapsed(7),
      withReferredDaysElapsed(6),
      withReference("13"),
      withStatus(CaseStatus.NEW),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withoutDecision()
    )
    val liabilities = List(c4, c5, c7, c9, c13)
    val liveLiabilities = List(c4, c5, c7, c9)
    val nonLiveLiabilities = List(c13)
    val atarCases = List(c1,c2,c3)

    "group by pseudo status" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(cases.traverse(repository.insert))
      await(liveCases.traverse(repository.insert))
      await(expiredCases.traverse(repository.insert))
      collectionSize shouldBe 10

      val report = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.COMPLETED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(8)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.EXPIRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(9)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.COMPLETED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(8))),
          cases     = List(c7, c8)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.EXPIRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(9))),
          cases     = List(c10, c9)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7))),
          cases     = List(c3, c4)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4))),
          cases     = List(c1, c2)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5))),
          cases     = List(c5, c6)
        )
      )
    }

    "group by liability status" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(liabilities.traverse(repository.insert))
      collectionSize shouldBe 5

      val report = SummaryReport(
        groupBy   = ReportField.LiabilityStatus,
        sortBy    = ReportField.LiabilityStatus
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 4,
          groupKey  = ReportField.LiabilityStatus.withValue(Some(LiabilityStatus.LIVE))
        ),
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.LiabilityStatus.withValue(Some(LiabilityStatus.NON_LIVE))

        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 4,
          groupKey  = ReportField.LiabilityStatus.withValue(Some(LiabilityStatus.LIVE)),
          cases     = liveLiabilities
        ),
        CaseResultGroup(
          count     = 1,
          groupKey  = ReportField.LiabilityStatus.withValue(Some(LiabilityStatus.NON_LIVE)),
          cases     = nonLiveLiabilities
        )
      )
    }

    "group by team" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.Team,
        sortBy    = ReportField.Team,
        maxFields = Seq(ReportField.ElapsedDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Team.withValue(Some("1")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(2)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Team.withValue(Some("2")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Team.withValue(Some("3")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Team.withValue(Some("4")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 1,
          groupKey  = ReportField.Team.withValue(Some("1")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(2))),
          cases     = List(c1)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Team.withValue(Some("2")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4))),
          cases     = List(c2, c3)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Team.withValue(Some("3")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7))),
          cases     = List(c4, c5)
        ),
        CaseResultGroup(
          count     = 1,
          groupKey  = ReportField.Team.withValue(Some("4")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5))),
          cases     = List(c6)
        )
      )
    }

    "group by assignee" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.User,
        sortBy    = ReportField.User,
        sortOrder = SortDirection.DESCENDING,
        maxFields = Seq(ReportField.TotalDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.User.withValue(Some("3")),
          maxFields = List(ReportField.TotalDays.withValue(Some(31)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.User.withValue(Some("2")),
          maxFields = List(ReportField.TotalDays.withValue(Some(32)))
        ),
        SimpleResultGroup(
          count     = 3,
          groupKey  = ReportField.User.withValue(Some("1")),
          maxFields = List(ReportField.TotalDays.withValue(Some(397)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 1,
          groupKey  = ReportField.User.withValue(Some("3")),
          maxFields = List(ReportField.TotalDays.withValue(Some(31))),
          cases     = List(c6)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.User.withValue(Some("2")),
          maxFields = List(ReportField.TotalDays.withValue(Some(32))),
          cases     = List(c4, c5)
        ),
        CaseResultGroup(
          count     = 3,
          groupKey  = ReportField.User.withValue(Some("1")),
          maxFields = List(ReportField.TotalDays.withValue(Some(397))),
          cases     = List(c1, c2, c3)
        )
      )
    }

    "group by case type" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.CaseType,
        sortBy    = ReportField.Count,
        sortOrder = SortDirection.DESCENDING,
        maxFields = Seq(ReportField.ReferredDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 4,
          groupKey  = ReportField.CaseType.withValue(Some(ApplicationType.BTI)),
          maxFields = List(ReportField.ReferredDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.CaseType.withValue(Some(ApplicationType.LIABILITY_ORDER)),
          maxFields = List(ReportField.ReferredDays.withValue(Some(6)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 4,
          groupKey  = ReportField.CaseType.withValue(Some(ApplicationType.BTI)),
          maxFields = List(ReportField.ReferredDays.withValue(Some(7))),
          cases     = List(c1, c2, c3, c6)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.CaseType.withValue(Some(ApplicationType.LIABILITY_ORDER)),
          maxFields = List(ReportField.ReferredDays.withValue(Some(6))),
          cases     = List(c4, c5)
        )
      )
    }

    "group by commodity code chapter" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.Chapter,
        sortBy    = ReportField.Count,
        sortOrder = SortDirection.ASCENDING,
        maxFields = Seq(ReportField.ElapsedDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Chapter.withValue(Some("85")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Chapter.withValue(None),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 3,
          groupKey  = ReportField.Chapter.withValue(Some("95")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 1,
          groupKey  = ReportField.Chapter.withValue(Some("85")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4))),
          cases     = List(c3)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.Chapter.withValue(None),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7))),
          cases     = List(c4, c6)
        ),
        CaseResultGroup(
          count     = 3,
          groupKey  = ReportField.Chapter.withValue(Some("95")),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4))),
          cases     = List(c1, c2, c5)
        )
      )
    }

    "group by total days" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.TotalDays,
        sortBy    = ReportField.TotalDays,
        sortOrder = SortDirection.DESCENDING,
        maxFields = Seq(ReportField.ElapsedDays)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(397)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(32)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(31)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5)))
        )
      )

      val pagedWithCases = await(repository.summaryReport(report.copy(includeCases = true), Pagination()))

      pagedWithCases.results shouldBe Seq(
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(397)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4))),
          cases     = List(c1, c2)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(32)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7))),
          cases     = List(c3, c4)
        ),
        CaseResultGroup(
          count     = 2,
          groupKey  = ReportField.TotalDays.withValue(Some(31)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5))),
          cases     = List(c5, c6)
        )
      )
    }

    "filter by case type" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        caseTypes = Set(ApplicationType.BTI)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5)))
        )
      )
    }

    "filter by pseudo status" in {
      await(cases.traverse(repository.insert))
      await(expiredCases.traverse(repository.insert))
      collectionSize shouldBe 8

      val report = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        statuses  = Set(PseudoCaseStatus.OPEN)
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        )
      )

      val expiredReport = SummaryReport(
        groupBy   = ReportField.User,
        sortBy    = ReportField.User,
        maxFields = Seq(ReportField.ElapsedDays),
        statuses  = Set(PseudoCaseStatus.EXPIRED)
      )

      val expiredPaged = await(repository.summaryReport(expiredReport, Pagination()))

      expiredPaged.results shouldBe Seq(
        SimpleResultGroup(
          1,
          ReportField.User.withValue(Some("2")),
          List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          1,
          ReportField.User.withValue(Some("3")),
          List(ReportField.ElapsedDays.withValue(Some(9)))
        ),
      )
    }

    "filter by teams" in {
      await(cases.traverse(repository.insert))
      await(gatewayCases.traverse(repository.insert))
      collectionSize shouldBe 8

      val report = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        teams     = Set("2", "3")
      )

      val paged = await(repository.summaryReport(report, Pagination()))

      paged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        ),
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        ),
        SimpleResultGroup(
          count     = 1,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        )
      )

      val gatewayReport = SummaryReport(
        groupBy   = ReportField.CaseType,
        sortBy    = ReportField.CaseType,
        maxFields = Seq(ReportField.ElapsedDays),
        teams     = Set("1")
      )

      val gatewayPaged = await(repository.summaryReport(gatewayReport, Pagination()))

      gatewayPaged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.CaseType.withValue(Some(ApplicationType.BTI)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(2)))
        )
      )
    }

    "filter by date range" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val maxDateReport = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        dateRange = InstantRange(Instant.MIN, Instant.parse("2020-06-30T09:00:00.00Z"))
      )

      val maxDatePaged = await(repository.summaryReport(maxDateReport, Pagination()))

      maxDatePaged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(4)))
        )
      )

      val minDateReport = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        dateRange = InstantRange(Instant.parse("2020-12-31T12:00:00.00Z"), Instant.MAX)
      )

      val minDatePaged = await(repository.summaryReport(minDateReport, Pagination()))

      minDatePaged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(5)))
        )
      )

      val minMaxDateReport = SummaryReport(
        groupBy   = ReportField.Status,
        sortBy    = ReportField.Status,
        maxFields = Seq(ReportField.ElapsedDays),
        dateRange = InstantRange(Instant.parse("2020-06-30T09:00:00.00Z"), Instant.parse("2020-12-31T12:00:00.00Z"))
      )

      val minMaxDatePaged = await(repository.summaryReport(minMaxDateReport, Pagination()))

      minMaxDatePaged.results shouldBe Seq(
        SimpleResultGroup(
          count     = 2,
          groupKey  = ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          maxFields = List(ReportField.ElapsedDays.withValue(Some(7)))
        )
      )
    }
  }

  "CaseReport" should {
    val c1 = aCase(
      withQueue("1"),
      withActiveDaysElapsed(2),
      withReferredDaysElapsed(1),
      withReference("1"),
      withStatus(CaseStatus.OPEN),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9506999000")
    )
    val c2 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(2),
      withReference("2"),
      withStatus(CaseStatus.OPEN),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9507900000")
    )
    val c3 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(7),
      withReference("3"),
      withStatus(CaseStatus.NEW),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withDecision("8518300090")
    )
    val c4 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(7),
      withReferredDaysElapsed(6),
      withReference("4"),
      withStatus(CaseStatus.NEW),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withoutDecision()
    )
    val c5 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("5"),
      withStatus(CaseStatus.REFERRED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val c6 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(5),
      withReferredDaysElapsed(0),
      withReference("6"),
      withStatus(CaseStatus.REFERRED),
      withAssignee(Some(Operator("3"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withoutDecision()
    )
    val cases = List(c1, c2, c3, c4, c5, c6)

    "filter by case type" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = CaseReport(
        sortBy    = ReportField.Reference,
        fields    = Seq(ReportField.Reference, ReportField.DateCreated, ReportField.ElapsedDays),
        caseTypes = Set(ApplicationType.BTI)
      )

      val paged = await(repository.caseReport(report, Pagination()))

      paged.results shouldBe Seq(
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("1")),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2020-01-01T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(2))
        ),
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("2")),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2020-01-01T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(4))
        ),
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("3")),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2020-12-31T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(4))
        ),
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("6")),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2021-01-01T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(5))
        )
      )
    }

    "filter by pseudo status" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = CaseReport(
        sortBy   = ReportField.Reference,
        fields   = Seq(ReportField.Reference, ReportField.Status, ReportField.DateCreated, ReportField.ElapsedDays),
        statuses = Set(PseudoCaseStatus.OPEN)
      )

      val paged = await(repository.caseReport(report, Pagination()))

      paged.results shouldBe Seq(
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("1")),
          "status"       -> ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2020-01-01T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(2))
        ),
        Map(
          "reference"    -> ReportField.Reference.withValue(Some("2")),
          "status"       -> ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          "date_created" -> ReportField.DateCreated.withValue(Some(Instant.parse("2020-01-01T09:00:00.00Z"))),
          "elapsed_days" -> ReportField.ElapsedDays.withValue(Some(4))
        )
      )
    }

    "filter by teams" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val report = CaseReport(
        sortBy    = ReportField.Reference,
        sortOrder = SortDirection.DESCENDING,
        fields    = Seq(ReportField.Reference, ReportField.Chapter, ReportField.User, ReportField.TotalDays),
        teams     = Set("2", "3")
      )

      val paged = await(repository.caseReport(report, Pagination()))

      paged.results shouldBe Seq(
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("5")),
          "chapter"       -> ReportField.Chapter.withValue(Some("95")),
          "assigned_user" -> ReportField.User.withValue(Some("2")),
          "total_days"    -> ReportField.TotalDays.withValue(Some(31))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("4")),
          "chapter"       -> ReportField.Chapter.withValue(None),
          "assigned_user" -> ReportField.User.withValue(Some("2")),
          "total_days"    -> ReportField.TotalDays.withValue(Some(32))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("3")),
          "chapter"       -> ReportField.Chapter.withValue(Some("85")),
          "assigned_user" -> ReportField.User.withValue(Some("1")),
          "total_days"    -> ReportField.TotalDays.withValue(Some(32))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("2")),
          "chapter"       -> ReportField.Chapter.withValue(Some("95")),
          "assigned_user" -> ReportField.User.withValue(Some("1")),
          "total_days"    -> ReportField.TotalDays.withValue(Some(397))
        )
      )
    }

    "filter by date range" in {
      await(cases.traverse(repository.insert))
      collectionSize shouldBe 6

      val maxDateReport = CaseReport(
        sortBy    = ReportField.Status,
        fields    = Seq(ReportField.Reference, ReportField.Status, ReportField.Team, ReportField.ReferredDays),
        dateRange = InstantRange(Instant.MIN, Instant.parse("2020-06-30T09:00:00.00Z"))
      )

      val maxDatePaged = await(repository.caseReport(maxDateReport, Pagination()))

      maxDatePaged.results shouldBe Seq(
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("1")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          "assigned_team" -> ReportField.Team.withValue(Some("1")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(1))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("2")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.OPEN)),
          "assigned_team" -> ReportField.Team.withValue(Some("2")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(2))
        )
      )

      val minDateReport = CaseReport(
        sortBy    = ReportField.Status,
        fields    = Seq(ReportField.Reference, ReportField.Status, ReportField.Team, ReportField.ReferredDays),
        dateRange = InstantRange(Instant.parse("2020-12-31T12:00:00.00Z"), Instant.MAX)
      )

      val minDatePaged = await(repository.caseReport(minDateReport, Pagination()))

      minDatePaged.results shouldBe Seq(
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("5")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          "assigned_team" -> ReportField.Team.withValue(Some("3")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(3))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("6")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.REFERRED)),
          "assigned_team" -> ReportField.Team.withValue(Some("4")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(0))
        )
      )

      val minMaxDateReport = CaseReport(
        sortBy    = ReportField.Status,
        fields    = Seq(ReportField.Reference, ReportField.Status, ReportField.Team, ReportField.ReferredDays),
        dateRange = InstantRange(Instant.parse("2020-06-30T09:00:00.00Z"), Instant.parse("2020-12-31T12:00:00.00Z"))
      )

      val minMaxDatePaged = await(repository.caseReport(minMaxDateReport, Pagination()))

      minMaxDatePaged.results shouldBe Seq(
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("3")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          "assigned_team" -> ReportField.Team.withValue(Some("2")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(7))
        ),
        Map(
          "reference"     -> ReportField.Reference.withValue(Some("4")),
          "status"        -> ReportField.Status.withValue(Some(PseudoCaseStatus.NEW)),
          "assigned_team" -> ReportField.Team.withValue(Some("3")),
          "referred_days" -> ReportField.ReferredDays.withValue(Some(6))
        )
      )
    }
  }

  "QueueReport" should {
    val c1 = aCase(
      withQueue("1"),
      withActiveDaysElapsed(2),
      withReferredDaysElapsed(1),
      withReference("1"),
      withStatus(CaseStatus.OPEN),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9506999000")
    )
    val c2 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(2),
      withReference("2"),
      withStatus(CaseStatus.OPEN),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2020-01-01T09:00:00.00Z")),
      withDecision("9507900000")
    )
    val c3 = aCase(
      withQueue("2"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(7),
      withReference("3"),
      withStatus(CaseStatus.NEW),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withDecision("8518300090")
    )
    val c4 = aCase(
      withQueue("3"),
      withActiveDaysElapsed(7),
      withReferredDaysElapsed(6),
      withReference("4"),
      withStatus(CaseStatus.NEW),
      withCreatedDate(Instant.parse("2020-12-31T09:00:00.00Z")),
      withoutDecision()
    )
    val c5 = aCase(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("5"),
      withStatus(CaseStatus.REFERRED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val c6 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(5),
      withReferredDaysElapsed(0),
      withReference("6"),
      withStatus(CaseStatus.REFERRED),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withoutDecision()
    )
    val cases = List(c1, c2, c3, c4, c5, c6)

    val c7 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("7"),
      withStatus(CaseStatus.COMPLETED),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val c8 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(8),
      withReferredDaysElapsed(0),
      withReference("8"),
      withStatus(CaseStatus.COMPLETED),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision("9507209000")
    )
    val liveCases = List(c7, c8)

    val c9 = aCase(liabCase1)(
      withQueue("3"),
      withActiveDaysElapsed(4),
      withReferredDaysElapsed(3),
      withReference("9"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("1"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision(
        "9507209000",
        effectiveStartDate = Some(Instant.parse("2018-01-31T09:00:00.00Z")),
        effectiveEndDate   = Some(Instant.parse("2021-01-31T09:00:00.00Z"))
      )
    )
    val c10 = aCase(
      withQueue("4"),
      withActiveDaysElapsed(9),
      withReferredDaysElapsed(0),
      withReference("10"),
      withStatus(CaseStatus.COMPLETED),
      withAssignee(Some(Operator("2"))),
      withCreatedDate(Instant.parse("2021-01-01T09:00:00.00Z")),
      withDecision(
        "9507209000",
        effectiveStartDate = Some(Instant.parse("2018-01-31T09:00:00.00Z")),
        effectiveEndDate   = Some(Instant.parse("2021-01-31T09:00:00.00Z"))
      )
    )
    val expiredCases = List(c9, c10)

    "group unassigned cases by team and case type" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(cases.traverse(repository.insert))
      await(liveCases.traverse(repository.insert))
      await(expiredCases.traverse(repository.insert))
      collectionSize shouldBe 10

      val report = QueueReport(sortOrder = SortDirection.DESCENDING)

      val paged = await(repository.queueReport(report, Pagination()))

      paged.results shouldBe Seq(
        QueueResultGroup(2,Some("4"), ApplicationType.BTI),
        QueueResultGroup(1,Some("3"), ApplicationType.LIABILITY_ORDER),
        QueueResultGroup(1,Some("3"), ApplicationType.BTI),
        QueueResultGroup(1,Some("2"), ApplicationType.BTI),
        QueueResultGroup(1,Some("1"), ApplicationType.BTI),
      )
    }

    "sort unassigned cases by count" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(cases.traverse(repository.insert))
      await(liveCases.traverse(repository.insert))
      await(expiredCases.traverse(repository.insert))
      collectionSize shouldBe 10

      val report = QueueReport(sortBy = ReportField.Count)

      val paged = await(repository.queueReport(report, Pagination()))

      paged.results shouldBe Seq(
        QueueResultGroup(1,Some("1"), ApplicationType.BTI),
        QueueResultGroup(1,Some("2"), ApplicationType.BTI),
        QueueResultGroup(1,Some("3"), ApplicationType.BTI),
        QueueResultGroup(1,Some("3"), ApplicationType.LIABILITY_ORDER),
        QueueResultGroup(2,Some("4"), ApplicationType.BTI),
      )
    }

    "filter by assignee" in {
      given(config.clock) willReturn Clock.fixed(Instant.parse("2021-02-01T09:00:00.00Z"), ZoneOffset.UTC)

      await(cases.traverse(repository.insert))
      await(liveCases.traverse(repository.insert))
      await(expiredCases.traverse(repository.insert))
      collectionSize shouldBe 10

      val reportPid1 = QueueReport(assignee = Some("1"))

      val pagedPid1 = await(repository.queueReport(reportPid1, Pagination()))

      pagedPid1.results shouldBe Seq(
        QueueResultGroup(1,Some("2"), ApplicationType.BTI),
        QueueResultGroup(1,Some("3"), ApplicationType.LIABILITY_ORDER),
      )

      val reportPid2 = QueueReport(assignee = Some("2"))

      val pagedPid2 = await(repository.queueReport(reportPid2, Pagination()))

      pagedPid2.results shouldBe Seq(
        QueueResultGroup(1,Some("3"), ApplicationType.BTI),
        QueueResultGroup(1,Some("4"), ApplicationType.BTI)
      )
    }
  }

  "The 'cases' collection" should {

    "have a unique index based on the field 'reference' " in {
      await(repository.insert(case1))
      val size = collectionSize

      val caught = intercept[DatabaseException] {

        await(repository.insert(case1.copy(status = CaseStatus.REFERRED)))
      }
      caught.code shouldBe Some(conflict)

      collectionSize shouldBe size
    }

    "store dates as Mongo Dates" in {
      val date    = Instant.now()
      val oldCase = case1.copy(createdDate = date)
      val newCase = case2.copy(createdDate = date.plusSeconds(1))
      await(repository.insert(oldCase))
      await(repository.insert(newCase))

      def selectAllWithSort(dir: Int): Future[Seq[Case]] = getMany(Json.obj(), Json.obj("createdDate" -> dir))

      await(selectAllWithSort(1))  shouldBe Seq(oldCase, newCase)
      await(selectAllWithSort(-1)) shouldBe Seq(newCase, oldCase)
    }

    "have all expected indexes" in {

      import scala.concurrent.duration._

      val expectedIndexes = List(
        Index(key = Seq("_id"       -> Ascending), name = Some("_id_")),
        Index(key = Seq("reference" -> Ascending), name = Some("reference_Index"), unique = true),
        Index(key = Seq("queueId"   -> Ascending), name = Some("queueId_Index"), unique = false),
        Index(
          key    = Seq("application.holder.eori" -> Ascending),
          name   = Some("application.holder.eori_Index"),
          unique = false
        ),
        Index(
          key    = Seq("application.agent.eoriDetails.eori" -> Ascending),
          name   = Some("application.agent.eoriDetails.eori_Index"),
          unique = false
        ),
        Index(key = Seq("daysElapsed" -> Ascending), name = Some("daysElapsed_Index"), unique = false),
        Index(key = Seq("assignee.id" -> Ascending), name = Some("assignee.id_Index"), unique = false),
        Index(
          key    = Seq("decision.effectiveEndDate" -> Ascending),
          name   = Some("decision.effectiveEndDate_Index"),
          unique = false
        ),
        Index(
          key    = Seq("decision.bindingCommodityCode" -> Ascending),
          name   = Some("decision.bindingCommodityCode_Index"),
          unique = false
        ),
        Index(key = Seq("status"   -> Ascending), name = Some("status_Index"), unique   = false),
        Index(key = Seq("keywords" -> Ascending), name = Some("keywords_Index"), unique = false)
      )

      val repo = newMongoRepository
      await(repo.ensureIndexes)

      eventually(timeout(5.seconds), interval(100.milliseconds)) {
        assertIndexes(expectedIndexes.sorted, getIndexes(repo.collection).sorted)
      }

      await(repo.drop)
    }
  }

  protected def getMany(filterBy: JsObject, sortBy: JsObject): Future[Seq[Case]] =
    repository.collection
      .find[JsObject, Case](filterBy)
      .sort(sortBy)
      .cursor[Case]()
      .collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[Case]]())

  private def selectorByReference(c: Case) =
    BSONDocument("reference" -> c.reference)

  private def store(cases: Case*): Unit =
    cases.foreach { c: Case => await(repository.insert(c)) }

}
