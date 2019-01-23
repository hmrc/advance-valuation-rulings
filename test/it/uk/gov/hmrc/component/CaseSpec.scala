/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.component

import java.time.ZonedDateTime

import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.HttpVerbs
import play.api.http.Status._
import play.api.libs.json.Json
import scalaj.http.{Http, HttpResponse}
import uk.gov.hmrc.bindingtariffclassification.model.RESTFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import util.CaseData._
import util.Matchers.roughlyBe

class CaseSpec extends BaseFeatureSpec {

  override lazy val port = 14681
  protected val serviceUrl = s"http://localhost:$port"

  private val q1 = "queue1"
  private val u1 = Operator("user1")
  private val c0 = createNewCase(app = createBasicBTIApplication)
  private val c1 = createCase(app = createBasicBTIApplication, queue = Some(q1), assignee = Some(u1))
  private val status = CaseStatus.CANCELLED
  private val c1_updated = c1.copy(status = status)
  private val c2 = createCase(app = createLiabilityOrder,
    decision = Some(createDecision),
    attachments = Seq(createAttachment,createAttachmentWithOperator))
  private val c3 = createNewCaseWithExtraFields()
  private val c4 = createNewCase(app = createBTIApplicationWithAllFields)

  private val c0Json = Json.toJson(c0)
  private val c1Json = Json.toJson(c1)
  private val c1UpdatedJson = Json.toJson(c1_updated)
  private val c3Json = Json.toJson(c3)
  private val c4Json = Json.toJson(c4)


  feature("Delete All") {

    scenario("Clear Collection") {

      Given("There are some documents in the collection")
      storeCases(c1, c2)

      When("I delete all documents")
      val deleteResult = Http(s"$serviceUrl/cases")
        .method(HttpVerbs.DELETE)
        .asString

      Then("The response code should be 204")
      deleteResult.code shouldEqual NO_CONTENT

      And("The response body is empty")
      deleteResult.body shouldBe ""

      And("No documents exist in the mongo collection")
      caseStoreSize shouldBe 0
    }

  }


  feature("Create Case") {

    scenario("Create a new case") {

      When("I create a new case")
      val result: HttpResponse[String] = Http(s"$serviceUrl/cases")
        .headers(Seq(CONTENT_TYPE -> JSON))
        .postData(c0Json.toString()).asString

      Then("The response code should be created")
      result.code shouldEqual CREATED

      And("The case is returned in the JSON response")
      val responseCase = Json.parse(result.body).as[Case]
      responseCase.reference shouldBe "1"
      responseCase.status shouldBe CaseStatus.NEW
    }

    scenario("Extra fields are ignored when creating a case") {
      When("I create a new case with extra fields")
      val result: HttpResponse[String] = Http(s"$serviceUrl/cases")
        .headers(Seq(CONTENT_TYPE -> JSON))
        .postData(c3Json.toString()).asString

      Then("The response code should be created")
      result.code shouldEqual CREATED

      And("The case is returned in the JSON response")
      val responseCase = Json.parse(result.body).as[Case]
      responseCase.reference shouldBe "1"
      responseCase.status shouldBe CaseStatus.NEW
      responseCase.createdDate should roughlyBe(ZonedDateTime.now())
      responseCase.assignee shouldBe None
      responseCase.queueId shouldBe None
      responseCase.decision shouldBe None
      responseCase.closedDate shouldBe None
    }

    scenario("Create a new case with all fields") {

      When("I create a new case")
      val result: HttpResponse[String] = Http(s"$serviceUrl/cases")
        .headers(Seq(CONTENT_TYPE -> JSON))
        .postData(c4Json.toString()).asString

      Then("The response code should be created")
      result.code shouldEqual CREATED

      And("The case is returned in the JSON response")
      val responseCase = Json.parse(result.body).as[Case]
      responseCase.reference shouldBe "1"
      responseCase.status shouldBe CaseStatus.NEW
    }

  }


  feature("Update Case") {

    scenario("Update an non-existing case") {

      When("I update a non-existing case")
      val result = Http(s"$serviceUrl/cases/${c1.reference}")
        .headers(Seq(CONTENT_TYPE -> JSON))
        .put(c1Json.toString()).asString

      Then("The response code should be NOT FOUND")
      result.code shouldEqual NOT_FOUND
    }

    scenario("Update an existing case") {

      Given("There is a case in the database")
      storeCases(c1)

      When("I update an existing case")
      val result = Http(s"$serviceUrl/cases/${c1.reference}")
        .headers(Seq(CONTENT_TYPE -> JSON))
        .put(c1UpdatedJson.toString()).asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The case is returned in the JSON response")
      Json.parse(result.body) shouldBe c1UpdatedJson
    }

  }

  feature("Get Case") {

    scenario("Get existing case") {

      Given("There is a case in the database")
      storeCases(c1)

      When("I get a case")
      val result = Http(s"$serviceUrl/cases/${c1.reference}").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The case is returned in the JSON response")
      Json.parse(result.body) shouldBe c1Json
    }

    scenario("Get a non-existing case") {

      When("I get a case")
      val result = Http(s"$serviceUrl/cases/${c1.reference}").asString

      Then("The response code should be NOT FOUND")
      result.code shouldEqual NOT_FOUND
    }

  }


  feature("Get Cases") {

    scenario("Get all cases") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get all cases")
      val result = Http(s"$serviceUrl/cases").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c1, c2))
    }

    scenario("Get no cases") {

      Given("There are no cases in the database")

      When("I get all cases")
      val result = Http(s"$serviceUrl/cases").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("No cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq.empty[Case])
    }

  }

  // TODO: test all possible combinations of get()

  feature("Get Cases by Queue Id") {

    scenario("Filtering cases that have undefined queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by queue id")
      val result = Http(s"$serviceUrl/cases?queue_id=none").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c2))
    }

    scenario("Filtering cases by a valid queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by queue id")
      val result = Http(s"$serviceUrl/cases?queue_id=$q1").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c1))
    }

    scenario("Filtering cases by a wrong queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by queue id")
      val result = Http(s"$serviceUrl/cases?queue_id=wrong").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("No cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq.empty[Case])
    }

  }


  feature("Get Cases sorted by days elapsed") {
    val oldCase = c1.copy(daysElapsed = 1)
    val newCase = c2.copy(daysElapsed = 0)

    scenario("Sorting default") {
      Given("There are few cases in the database")
      storeCases(oldCase, newCase)

      When("I get all cases sorted by elapsed days")
      val result = Http(s"$serviceUrl/cases?sort_by=days-elapsed").asString

      Then("The response code should be 200")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(oldCase, newCase))
    }

    scenario("Sorting ascending") {
      Given("There are few cases in the database")
      storeCases(oldCase, newCase)

      When("I get all cases sorted by elapsed days")
      val result = Http(s"$serviceUrl/cases?sort_by=days-elapsed&sort_direction=ascending").asString

      Then("The response code should be 200")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(newCase, oldCase))
    }

    scenario("Sorting descending") {
      Given("There are few cases in the database")
      storeCases(oldCase, newCase)

      When("I get all cases sorted by elapsed days")
      val result = Http(s"$serviceUrl/cases?sort_by=days-elapsed&sort_direction=descending").asString

      Then("The response code should be 200")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(oldCase, newCase))
    }

  }


  feature("Get Cases by Assignee Id") {

    scenario("Filtering cases that have undefined assigneeId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id")
      val result = Http(s"$serviceUrl/cases?assignee_id=none").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c2))
    }

    scenario("Filtering cases by a valid assigneeId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id")
      val result = Http(s"$serviceUrl/cases?assignee_id=$u1").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c1))
    }

    scenario("Filtering cases by a wrong assigneeId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id")
      val result = Http(s"$serviceUrl/cases?assignee_id=wrong").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("No cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq.empty[Case])
    }

  }


  feature("Get Cases by Assignee Id and Queue Id") {

    scenario("Filtering cases that have undefined assigneeId and undefined queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id and queue id")
      val result = Http(s"$serviceUrl/cases?assignee_id=none&queue_id=none").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c2))
    }

    scenario("Filtering cases by a valid assigneeId and a valid queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id and queue id")
      val result = Http(s"$serviceUrl/cases?assignee_id=$u1&queue_id=$q1").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("The expected cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq(c1))
    }

    scenario("Filtering cases by a wrong assigneeId and a valid queueId") {

      Given("There are few cases in the database")
      storeCases(c1, c2)

      When("I get cases by assignee id")
      val result = Http(s"$serviceUrl/cases?assignee_id=_a_&queue_id=$q1").asString

      Then("The response code should be OK")
      result.code shouldEqual OK

      And("No cases are returned in the JSON response")
      Json.parse(result.body) shouldBe Json.toJson(Seq.empty[Case])
    }

  }

}
