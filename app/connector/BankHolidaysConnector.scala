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

package connector

import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.Json
import common.Logging
import config.AppConfig
import http.ProxyHttpClient
import metrics.HasMetrics
import model.BankHolidaysResponse
import model.RESTFormatters.formatBankHolidaysResponse
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class BankHolidaysConnector @Inject() (appConfig: AppConfig, http: ProxyHttpClient, val metrics: Metrics)(
  implicit
  executionContext: ExecutionContext
) extends Logging
    with HasMetrics {
  def get()(implicit headerCarrier: HeaderCarrier): Future[Set[LocalDate]] =
    withMetricsTimerAsync("get-bank-holidays") { _ =>
      http
        .GET[BankHolidaysResponse](appConfig.bankHolidaysUrl)
        .recover(withResourcesFile)
        .map(_.`england-and-wales`.events.map(_.date).toSet)
    }

  private def withResourcesFile: PartialFunction[Throwable, BankHolidaysResponse] = {
    case t =>
      logger.error("Bank Holidays Request Failed", t)
      val url    = getClass.getClassLoader.getResource("bank-holidays-fallback.json")
      val source = Source.fromURL(url, StandardCharsets.UTF_8.name())
      val content =
        try source.getLines().mkString
        finally source.close()
      Json.fromJson(Json.parse(content)).get
  }

}
