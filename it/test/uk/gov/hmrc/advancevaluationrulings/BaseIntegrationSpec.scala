/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor1}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, EitherValues}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Query
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URLEncoder
import scala.concurrent.ExecutionContext

trait BaseIntegrationSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with TableDrivenPropertyChecks
    with BeforeAndAfterEach {

  given headerCarrier: HeaderCarrier = HeaderCarrier()

  val baseUrl               = s"http://localhost:$port"
  val traderDetailsEndpoint = s"$baseUrl/advance-valuation-rulings/trader-details"
  val submitAnswersEndpoint = s"$baseUrl/advance-valuation-rulings/submit-answers"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .configure("auditing.enabled" -> false)
      .configure("microservice.services.integration-framework.port" -> WireMockHelper.wireMockPort)
      .build()

  given ec: ExecutionContext        = fakeApplication().injector.instanceOf[ExecutionContext]
  lazy val wsClient: WSClient       = fakeApplication().injector.instanceOf[WSClient]
  lazy val httpClient: HttpClientV2 = fakeApplication().injector.instanceOf[HttpClientV2]
  lazy val appConfig: AppConfig     = fakeApplication().injector.instanceOf[AppConfig]
  lazy val ETMPEndpoint: String     = appConfig.etmpSubscriptionDisplayEndpoint

  val requestHeaders: Set[(String, String)] = Set(
    ("Authorization", s"Bearer ${appConfig.integrationFrameworkToken}")
  )

  def etmpQueryUrl(query: Query): String =
    s"$ETMPEndpoint?" +
      s"regime=${URLEncoder.encode(query.regime.entryName, "UTF-8")}" +
      s"&acknowledgementReference=${URLEncoder.encode(query.acknowledgementReference, "UTF-8")}" +
      s"&EORI=${URLEncoder.encode(query.EORI.value, "UTF-8")}"

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

  def traderDetailsRequestUrl(acknowledgementReference: String, eoriNumber: String): String =
    s"$traderDetailsEndpoint/${URLEncoder.encode(acknowledgementReference, "UTF-8")}/${URLEncoder
        .encode(eoriNumber, "UTF-8")}"
}
