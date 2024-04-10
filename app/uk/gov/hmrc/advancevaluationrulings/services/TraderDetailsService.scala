/*
 * Copyright 2024 HM Revenue & Customs
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

import uk.gov.hmrc.advancevaluationrulings.connectors.ETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.{AcknowledgementReference, EoriNumber}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{Query, Regime}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TraderDetailsService @Inject() (connector: ETMPConnector) {

  def getTraderDetails(
    acknowledgementReference: AcknowledgementReference,
    eoriNumber: EoriNumber
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[TraderDetailsResponse]] =
    connector
      .getSubscriptionDetails(
        Query(Regime.CDS, acknowledgementReference.value, EORI = Option(eoriNumber.value))
      )
      .map { response =>
        response.subscriptionDisplayResponse.responseDetail.map(responseDetail =>
          TraderDetailsResponse(
            responseDetail.EORINo,
            responseDetail.CDSFullName,
            responseDetail.CDSEstablishmentAddress,
            responseDetail.consentToDisclosureOfPersonalData.exists(_.equalsIgnoreCase("1")),
            responseDetail.contactInformation
          )
        )
      }

}
