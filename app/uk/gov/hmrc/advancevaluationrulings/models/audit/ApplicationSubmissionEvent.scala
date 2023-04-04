package uk.gov.hmrc.advancevaluationrulings.models.audit

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.advancevaluationrulings.models.application.Application
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole}

final case class ApplicationSubmissionEvent(
                                             internalId: String,
                                             affinityGroup: AffinityGroup,
                                             credentialRole: Option[CredentialRole],
                                             application: Application
                                           )

object ApplicationSubmissionEvent {

  implicit lazy val format: OFormat[ApplicationSubmissionEvent] = Json.format
}
