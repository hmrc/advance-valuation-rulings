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

import javax.inject.{Inject, Singleton}

import scala.concurrent.ExecutionContext

import play.api.libs.json.Writes
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.errors.{ConnectorError, ETMPError}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayRequest, ETMPSubscriptionDisplayResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}

import cats.data.EitherT
import cats.implicits.catsSyntaxEitherId

@Singleton
class DefaultETMPConnector @Inject() (httpClient: HttpClient, appConfig: AppConfig)
    extends ETMPConnector
    with HttpReaderWrapper[ETMPSubscriptionDisplayResponse, ETMPError] {

  def getSubscriptionDetails(
    request: ETMPSubscriptionDisplayRequest
  )(implicit
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Envelope[ETMPSubscriptionDisplayResponse] = {

    val baseUrl                = appConfig.integrationFrameworkBaseUrl
    val path                   = appConfig.etmpSubscriptionDisplayEndpoint
    val url                    = s"$baseUrl$path"
    val headerCarrierWithToken = headerCarrier.withExtraHeaders(
      "environment"   -> appConfig.integrationFrameworkEnv,
      "Authorization" -> s"Bearer ${appConfig.integrationFrameworkToken}"
    )

    withHttpReader {
      implicit reader =>
        implicit val httpWriter: Writes[ETMPSubscriptionDisplayRequest] =
          ETMPSubscriptionDisplayRequest.format
        EitherT {
          httpClient
            .POST(url, request)(httpWriter, reader, headerCarrierWithToken, ec)
            .recover {
              case ex: HttpException         =>
                ConnectorError(ex.getMessage).asLeft[ETMPSubscriptionDisplayResponse]
              case ex: UpstreamErrorResponse =>
                ConnectorError(ex.getMessage).asLeft[ETMPSubscriptionDisplayResponse]
              case otherException            =>
                ConnectorError(otherException.getMessage).asLeft[ETMPSubscriptionDisplayResponse]
            }
        }
    }
  }

}
