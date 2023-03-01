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

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import play.api.http.MimeTypes
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.logging.RequestAwareLogger
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.common.HeaderNames
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ConnectorError, ETMPError}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayResponse, Query}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import cats.data.EitherT
import cats.implicits.catsSyntaxEitherId

@Singleton
class DefaultETMPConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)
    extends ETMPConnector
    with HttpReaderWrapper[ETMPSubscriptionDisplayResponse, ETMPError] {

  override protected lazy val logger: RequestAwareLogger = new RequestAwareLogger(this.getClass)

  val dateFormat: DateTimeFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

  def getSubscriptionDetails(etmpQuery: Query)(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Envelope[ETMPSubscriptionDisplayResponse] = {

    val baseUrl = appConfig.integrationFrameworkBaseUrl
    val path    = appConfig.etmpSubscriptionDisplayEndpoint
    val url     = s"$baseUrl$path"
    val headers = Seq(
      HeaderNames.Authorization -> s"Bearer ${appConfig.integrationFrameworkToken}",
      HeaderNames.ForwardedHost -> "MDTP",
      HeaderNames.Accept        -> MimeTypes.JSON,
      HeaderNames.Date          -> LocalDateTime.now().atOffset(ZoneOffset.UTC).format(dateFormat)
    )

    withHttpReader {
      implicit reader =>
        EitherT {
          httpClient
            .GET(url, etmpQuery.toQueryParameters, headers)
            .recover {
              case ex =>
                logger.error(s"Failed to get subscription details from ETMP: ${ex.getMessage}")
                ConnectorError(ex.getMessage).asLeft[ETMPSubscriptionDisplayResponse]
            }
        }
    }
  }

}
