/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.controllers

import cats.data.OptionT
import cats.implicits.catsSyntaxOptionId
import generators.ModelGenerators
import org.mockito.ArgumentMatchers.{any, refEq, startsWith}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatest.matchers.must.Matchers.have
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status
import play.api.libs.json.{JsResult, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.advancevaluationrulings.controllers.ValuationCaseController.{AssignCaseRequest, AssignNewCaseRequest, CreateValuationRequest}
import uk.gov.hmrc.advancevaluationrulings.models.{AgentDetails, Attachment, CaseStatus, CaseWorker, Contact, EORIDetails, ValuationApplication, ValuationCase, ValuationRulingsApplication}
import uk.gov.hmrc.advancevaluationrulings.repositories.{ValuationCaseRepository, ValuationRulingsRepositoryImpl}
import uk.gov.hmrc.advancevaluationrulings.services.{MongoValuationCaseService, ValuationCaseService}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import wolfendale.scalacheck.regexp.GenParser.word

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class ValuationCaseControllerSpec extends ControllerBaseSpec with ModelGenerators with PlayMongoRepositorySupport[ValuationCase]{
  override protected def repository: PlayMongoRepository[ValuationCase] = new ValuationCaseRepository(mongoComponent)

  private val valuationCaseService = new MongoValuationCaseService(new ValuationCaseRepository(mongoComponent))

  override def beforeEach(): Unit = {
    deleteAll().futureValue
    prepareDatabase()
  }

  val controller = new ValuationCaseController(mcc, valuationCaseService)

  val caseReference = "Phone Case"

  val caseWorker = CaseWorker("case worker 1")

  private def responseBodyString(result: Result): String = jsonBodyOf(result).toString()

  def getAssignCaseRequest: FakeRequest[AssignCaseRequest] = {
    val assignCaseRequest = AssignCaseRequest(caseReference, caseWorker)
    FakeRequest().withBody[AssignCaseRequest](assignCaseRequest)
  }

  def getAssignNewCaseRequest: FakeRequest[AssignNewCaseRequest] = {
    val assignNewCaseRequest = AssignNewCaseRequest(caseReference, caseWorker)
    FakeRequest().withBody[AssignNewCaseRequest](assignNewCaseRequest)
  }

  def getCreateValuationRequest(reference: String = caseReference): FakeRequest[CreateValuationRequest] = {
    val createValuationRequest = CreateValuationRequest(reference, valuationApplicationGen.sample.get)
    FakeRequest().withBody[CreateValuationRequest](createValuationRequest)
  }

  "Valuation Case Controller" should {
    "Create one case" in {
      val result: Result = await(controller.create()(getCreateValuationRequest()))

      status(result) shouldBe Status.OK

      responseBodyString(result) should include("BsonObjectId")
    }

    "Return all open cases" in {
      val _ = await(controller.create()(getCreateValuationRequest()))
      val _ = await(controller.assignNewCase()(getAssignNewCaseRequest))

      val result: Result = await(controller.allOpenCases()(FakeRequest()))

      val openCases = Json.parse(responseBodyString(result)).as[List[ValuationCase]]

      status(result) shouldBe Status.OK
      openCases should have size 1
      openCases.head.status shouldBe CaseStatus.OPEN
    }

    "Return all new cases" ignore { //Reinstate when controller.allNewCases is implemented

      await(controller.assignNewCase()(getAssignNewCaseRequest))
      await(controller.assignNewCase()(getAssignNewCaseRequest))
      await(controller.assignNewCase()(getAssignNewCaseRequest))

      val result: Result = await(controller.allNewCases()(FakeRequest()))

      val newCases = Json.parse(responseBodyString(result)).as[List[ValuationCase]]

      status(result) shouldBe Status.OK
      newCases should have size 3
      newCases.map(_.status).distinct should have size 3
      newCases.head.status shouldBe CaseStatus.OPEN
    }

    "Return all cases that match a reference" in {
      val caseRef = caseReference + " 2"
      val _ = await(controller.create()(getCreateValuationRequest(caseRef)))

      val result: Result = await(controller.findByReference(caseRef)(FakeRequest()))

      val matchedCase = Json.parse(responseBodyString(result)).as[ValuationCase]

      status(result) shouldBe Status.OK
      matchedCase.reference shouldBe caseRef
    }

    "Assign one case to a caseworker" in {
      val _ = await(controller.create()(getCreateValuationRequest(caseReference)))

      val result: Result = await(controller.assignCase()(getAssignCaseRequest))

      val response = Json.parse(responseBodyString(result)).as[Long]

      status(result) shouldBe Status.OK
      response shouldBe 1L
    }

    "Return all cases that match an assignee" in {
      val _ = await(controller.create()(getCreateValuationRequest(caseReference)))
      val _ = await(controller.assignCase()(getAssignCaseRequest))

      val result: Result = await(controller.findByAssignee(caseWorker.id)(FakeRequest()))

      val matchedCase = Json.parse(responseBodyString(result)).as[List[ValuationCase]]

      status(result) shouldBe Status.OK
      matchedCase should have size 1
      matchedCase.head.assignee shouldBe Some(caseWorker)
    }

    "Assign new case - change status to OPEN" in {
      val _ = await(controller.create()(getCreateValuationRequest()))

      val resultBefore: Result = await(controller.findByReference(caseReference)(FakeRequest()))
      val caseBefore = Json.parse(responseBodyString(resultBefore)).as[ValuationCase]

      val _ = await(controller.assignNewCase()(getAssignNewCaseRequest))

      val resultAfter: Result = await(controller.findByReference(caseReference)(FakeRequest()))
      val caseAfter = Json.parse(responseBodyString(resultAfter)).as[ValuationCase]

      status(resultBefore) shouldBe Status.OK
      status(resultAfter) shouldBe Status.OK
      caseBefore.status shouldBe CaseStatus.NEW
      caseAfter.status shouldBe CaseStatus.OPEN
    }

    "Un-assign case - remove assignee" ignore {
      val _ = await(controller.create()(getCreateValuationRequest()))
      val _ = await(controller.assignCase()(getCreateValuationRequest()))

      val resultBefore: Result = await(controller.findByReference(caseReference)(FakeRequest()))
      val caseBefore = Json.parse(responseBodyString(resultBefore)).as[ValuationCase]

      val _ = await(controller.unAssignCase()(getAssignCaseRequest))

      val resultAfter: Result = await(controller.findByReference(caseReference)(FakeRequest()))
      val caseAfter = Json.parse(responseBodyString(resultAfter)).as[ValuationCase]

      status(resultBefore)shouldBe Status.OK
      status(resultAfter) shouldBe Status.OK
      caseBefore.assignee shouldBe Some(caseWorker)
      caseAfter.assignee shouldBe None
    }
  }
}
