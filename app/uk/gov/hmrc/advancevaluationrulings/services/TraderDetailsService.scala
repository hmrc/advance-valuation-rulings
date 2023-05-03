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

import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{Query, Regime, ResponseDetail}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class TraderDetailsService @Inject() (connector: ETMPConnector)(implicit ec: ExecutionContext) {

  def getTraderDetails(
    acknowledgementReference: AcknowledgementReference,
    eoriNumber: EoriNumber
  )(implicit
    hc: HeaderCarrier
  ): Future[TraderDetailsResponse] = {
    connector
      .getSubscriptionDetails(Query(Regime.CDS, acknowledgementReference.value, EORI = Option(eoriNumber.value)))
      .map(response => TraderDetailsResponse(response.subscriptionDisplayResponse.responseDetail))
  }

}
