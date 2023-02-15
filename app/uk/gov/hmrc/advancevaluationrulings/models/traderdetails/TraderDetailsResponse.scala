package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import uk.gov.hmrc.advancevaluationrulings.models.etmp.CDSEstablishmentAddress

final case class TraderDetailsResponse(
  EORINo: String,
  CDSFullName: String,
  CDSEstablishmentAddress: CDSEstablishmentAddress
)
