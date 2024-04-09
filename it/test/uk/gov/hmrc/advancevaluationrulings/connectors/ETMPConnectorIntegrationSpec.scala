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

package uk.gov.hmrc.advancevaluationrulings.connectors

import generators.ITModelGenerators
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.{BaseIntegrationSpec, WireMockHelper}

class ETMPConnectorIntegrationSpec extends BaseIntegrationSpec with WireMockHelper with ITModelGenerators {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  private val testETMPConnector = new ETMPConnector(httpClient, appConfig)

  "DefaultETMPConnector" should {
    "get subscription details" in {
      ScalaCheckPropertyChecks.forAll(
        queryGen,
        etmpSubscriptionDisplayResponseGen
      ) { (request, successResponse) =>
        val expectedResponse = Json.stringify(Json.toJson(successResponse))
        stubGet(
          etmpQueryUrl(request),
          statusCode = 200,
          expectedResponse,
          requestHeaders
        )

        val response = testETMPConnector.getSubscriptionDetails(request).futureValue

        response mustBe successResponse
      }
    }

    "handle subscription details error response" in {
      ScalaCheckPropertyChecks.forAll(queryGen) { etmpQuery =>
        val expectedResponse = "foo"
        stubGet(
          etmpQueryUrl(etmpQuery),
          statusCode = 500,
          expectedResponse,
          requestHeaders
        )

        testETMPConnector.getSubscriptionDetails(etmpQuery).failed.futureValue
      }
    }

    forAll(statusCodes) { statusCode =>
      s"return a failed future when unable to parse ETMP response with statusCode $statusCode" in {
        ScalaCheckPropertyChecks.forAll(queryGen) { etmpQuery =>
          stubGet(
            etmpQueryUrl(etmpQuery),
            statusCode,
            responseBody = "{}",
            requestHeaders
          )

          testETMPConnector.getSubscriptionDetails(etmpQuery).failed.futureValue
        }
      }

      s"return a failed future when ETMP returns an invalid json with statusCode $statusCode" in {
        ScalaCheckPropertyChecks.forAll(queryGen) { etmpQuery =>
          stubGet(
            etmpQueryUrl(etmpQuery),
            statusCode,
            responseBody = "invalid json response",
            requestHeaders
          )

          testETMPConnector.getSubscriptionDetails(etmpQuery).failed.futureValue
        }
      }
    }
  }
}
