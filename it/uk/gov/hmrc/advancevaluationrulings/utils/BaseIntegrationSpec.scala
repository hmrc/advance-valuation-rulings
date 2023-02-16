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

package uk.gov.hmrc.advancevaluationrulings.utils

import scala.concurrent.ExecutionContext

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import akka.actor.ActorSystem
import org.scalatest.{BeforeAndAfterAll, EitherValues}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

trait BaseIntegrationSpec
    extends PlaySpec
    with ScalaFutures
    with IntegrationPatience
    with EitherValues
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll {

  implicit val system: ActorSystem               = ActorSystem()
  implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()

  val baseUrl               = s"http://localhost:$port"
  val traderDetailsEndpoint = s"$baseUrl/advance-valuation-rulings/trader-details"

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.enabled" -> false)
      .configure("auditing.enabled" -> false)
      .configure("microservice.services.integration-framework.port" -> WireMockHelper.wireMockPort)
      .build()

  implicit lazy val ec: ExecutionContext = fakeApplication().injector.instanceOf[ExecutionContext]
  lazy val wsClient: WSClient            = fakeApplication().injector.instanceOf[WSClient]
  lazy val httpClient: DefaultHttpClient = fakeApplication().injector.instanceOf[DefaultHttpClient]
  lazy val appConfig: AppConfig          = fakeApplication().injector.instanceOf[AppConfig]
  lazy val ETMPEndpoint: String          = appConfig.etmpSubscriptionDisplayEndpoint

  val requestHeaders: Set[(String, String)] = Set(
    ("environment", appConfig.integrationFrameworkEnv),
    ("Authorization", s"Bearer ${appConfig.integrationFrameworkToken}")
  )
}
