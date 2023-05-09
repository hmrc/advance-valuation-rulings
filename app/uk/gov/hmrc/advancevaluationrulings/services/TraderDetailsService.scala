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

package uk.gov.hmrc.advancevaluationrulings.services

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import uk.gov.hmrc.advancevaluationrulings.config.AppConfig
import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{Query, Regime}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.advancevaluationrulings.repositories.TraderDetailsRepository
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class TraderDetailsService @Inject() (connector: ETMPConnector, repository: TraderDetailsRepository, appConfig: AppConfig)(implicit ec: ExecutionContext) extends Logging {

  def getTraderDetails(acknowledgementReference: AcknowledgementReference, eoriNumber: EoriNumber)(implicit hc: HeaderCarrier): Future[TraderDetailsResponse] = {
    lazy val query = Query(Regime.CDS, acknowledgementReference.value, EORI = Option(eoriNumber.value))

    getCache(eoriNumber).flatMap {
      _.fold {
          logger.debug("Cache value not found. Fetching value")
          for {
            response <- fetchRemote(query)
            _        <- saveCache(response)
          } yield response
        } {
          cacheValue =>
            logger.debug("Using cached value")
            Future.successful(cacheValue)
        }
    }
  }
  private def getCache(eoriNumber: EoriNumber) =
    if (appConfig.traderDetailsCacheEnabled) repository.get(eoriNumber.value) else Future.successful(None)

  private def saveCache(traderDetails: TraderDetailsResponse) = repository.set(traderDetails)

  private def fetchRemote(query: Query)(implicit hc: HeaderCarrier) =
    connector
      .getSubscriptionDetails(query)
      .map(response => TraderDetailsResponse(response.subscriptionDisplayResponse.responseDetail))

}
