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

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.errors.{JsonSerializationError, ParseError}
import uk.gov.hmrc.advancevaluationrulings.utils.{BaseIntegrationSpec, WireMockHelper}

import generators.ModelGenerators
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ETMPConnectorIntegrationSpec extends ETMPConnectorIntegrationSpecSupport {

  override def beforeAll(): Unit = startWireMock()
  override def afterAll(): Unit  = stopWireMock()

  "DefaultETMPConnector" should {
    "get subscription details" in {
      ScalaCheckPropertyChecks.forAll(
        ETMPSubscriptionDisplayRequestGen,
        ETMPSubscriptionDisplayResponseGen
      ) {
        (request, successResponse) =>
          val expectedResponse = Json.stringify(Json.toJson(successResponse))
          stubPost(
            ETMPEndpoint,
            Json.toJson(request),
            statusCode = 200,
            expectedResponse,
            requestHeaders
          )

          val response = testETMPConnector.getSubscriptionDetails(request).value.futureValue

          response.value mustBe successResponse
      }
    }

    "handle subscription details error response" in {
      ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen, ETMPErrorGen) {
        (request, errorResponse) =>
          val expectedResponse = Json.stringify(Json.toJson(errorResponse))
          stubPost(
            ETMPEndpoint,
            Json.toJson(request),
            statusCode = 500,
            expectedResponse,
            requestHeaders
          )

          val response = testETMPConnector.getSubscriptionDetails(request).value.futureValue

          response.left.value mustBe errorResponse
      }
    }

    forAll(statusCodes) {
      statusCode =>
        s"return a ParseError when unable to parse ETMP response with statusCode $statusCode" in {
          ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen) {
            request =>
              stubPost(
                ETMPEndpoint,
                Json.toJson(request),
                statusCode,
                responseBody = "{}",
                requestHeaders
              )

              val response = testETMPConnector.getSubscriptionDetails(request).value.futureValue

              response.left.value mustBe a[ParseError]
          }
        }

        s"return a JsonSerializationError when ETMP returns an invalid json with statusCode $statusCode" in {
          ScalaCheckPropertyChecks.forAll(ETMPSubscriptionDisplayRequestGen) {
            request =>
              stubPost(
                ETMPEndpoint,
                Json.toJson(request),
                statusCode,
                responseBody = "invalid json response",
                requestHeaders
              )

              val response = testETMPConnector.getSubscriptionDetails(request).value.futureValue

              response.left.value mustBe a[JsonSerializationError]
          }
        }
    }
  }
}

trait ETMPConnectorIntegrationSpecSupport
    extends BaseIntegrationSpec
    with WireMockHelper
    with ModelGenerators
    with TableDrivenPropertyChecks {

  val testETMPConnector = new DefaultETMPConnector(httpClient, appConfig)

  val statusCodes: TableFor1[Int] = Table(
    "statusCodes",
    Status.OK,
    Status.INTERNAL_SERVER_ERROR,
    Status.SERVICE_UNAVAILABLE,
    Status.BAD_GATEWAY,
    Status.GATEWAY_TIMEOUT,
    Status.BAD_REQUEST,
    Status.UNAUTHORIZED,
    Status.FORBIDDEN,
    Status.NOT_FOUND
  )
}
