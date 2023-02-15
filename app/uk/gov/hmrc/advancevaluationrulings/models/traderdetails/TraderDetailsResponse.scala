package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.CDSEstablishmentAddress

final case class TraderDetailsResponse(
  EORINo: String,
  CDSFullName: String,
  CDSEstablishmentAddress: CDSEstablishmentAddress
)

object TraderDetailsResponse {
  implicit val format: OFormat[TraderDetailsResponse] = Json.format[TraderDetailsResponse]
}
