package uk.gov.hmrc.advancevaluationrulings.services

import uk.gov.hmrc.advancevaluationrulings.connectors.DefaultETMPConnector
import uk.gov.hmrc.advancevaluationrulings.models.common.Envelope.Envelope
import uk.gov.hmrc.advancevaluationrulings.models.etmp.{ETMPSubscriptionDisplayRequest, Params, Query, Regime}
import uk.gov.hmrc.advancevaluationrulings.models.traderdetails.TraderDetailsResponse
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, LocalDateTime}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TraderDetailsService @Inject() (connector: DefaultETMPConnector) {
  // call DefaultETMPConnector.getSubscriptionDetails(): ETMPSubscriptionDisplayRequest -> ETMPSubscriptionDisplayResponse

  def getTraderDetails(
    regime: Regime,
    acknowledgementReference: String,
    taxPayerID: Option[String] = None,
    EORI: Option[String] = None
  )(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Envelope[TraderDetailsResponse] = {

    val etmpSubscriptionDetailsRequest = ETMPSubscriptionDisplayRequest(
      Params(
        LocalDateTime.now(Clock.systemUTC()),
        Query(regime, acknowledgementReference, taxPayerID, EORI)
      )
    )

    connector
      .getSubscriptionDetails(etmpSubscriptionDetailsRequest)
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
}
