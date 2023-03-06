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

import scala.concurrent.ExecutionContext

import uk.gov.hmrc.advancevaluationrulings.connectors.DefaultETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{Query, Regime}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class TraderDetailsService @Inject() (connector: DefaultETMPConnector) {

  def getTraderDetails(
    acknowledgementReference: AcknowledgementReference,
    eoriNumber: EoriNumber
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Envelope[TraderDetailsResponse] =
    connector
      .getSubscriptionDetails(
        Query(Regime.CDS, acknowledgementReference.value, EORI = Option(eoriNumber.value))
      )
      .map {
        response =>
          val responseDetail = response.subscriptionDisplayResponse.responseDetail
          TraderDetailsResponse(
            responseDetail.EORINo,
            responseDetail.CDSFullName,
            responseDetail.CDSEstablishmentAddress
          )
      }
}
