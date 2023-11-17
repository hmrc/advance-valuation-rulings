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

import play.api.http.MimeTypes
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.common.HeaderNames
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayResponse, Query}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ETMPConnector @Inject() (httpClient: HttpClientV2, appConfig: AppConfig)(implicit
  ec: ExecutionContext
) {

  private val dateFormat: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

  def getSubscriptionDetails(etmpQuery: Query)(implicit
    headerCarrier: HeaderCarrier
  ): Future[ETMPSubscriptionDisplayResponse] = {

    val baseUrl     = appConfig.integrationFrameworkBaseUrl
    val path        = appConfig.etmpSubscriptionDisplayEndpoint
    val queryString = etmpQuery.toQueryParameters.map { case (k, v) => s"$k=$v" }.mkString("&")
    val fullUrl     = s"$baseUrl$path?$queryString"

    httpClient
      .get(url"$fullUrl")
      .setHeader(HeaderNames.Authorization -> s"Bearer ${appConfig.integrationFrameworkToken}")
      .setHeader(HeaderNames.ForwardedHost -> "MDTP")
      .setHeader(HeaderNames.CorrelationId -> UUID.randomUUID().toString)
      .setHeader(HeaderNames.Accept -> MimeTypes.JSON)
      .setHeader(
        HeaderNames.Date -> LocalDateTime.now().atOffset(ZoneOffset.UTC).format(dateFormat)
      )
      .execute[ETMPSubscriptionDisplayResponse]
  }
}
