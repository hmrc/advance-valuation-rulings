package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.Regime

final case class TraderDetailsRequest(
  regime: Regime,
  acknowledgementReference: String,
  taxPayerID: Option[String] = None,
  EORI: Option[String] = None
)


object TraderDetailsRequest {
  implicit val format: OFormat[TraderDetailsRequest] = Json.format[TraderDetailsRequest]
}