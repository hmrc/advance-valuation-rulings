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

package uk.gov.hmrc.advancevaluationrulings.models

import java.time.Instant

import play.api.libs.json.{Format, Json, OFormat}

object CancelReason extends Enumeration {
  type CancelReason = Value

  val ANNULLED, INVALIDATED_CODE_CHANGE, INVALIDATED_EU_MEASURE, INVALIDATED_NATIONAL_MEASURE,
    INVALIDATED_WRONG_CLASSIFICATION, INVALIDATED_OTHER, OTHER = Value

  def format(reason: CancelReason): String = {
    val message = reason match {
      case ANNULLED                         => s"Annulled"
      case INVALIDATED_CODE_CHANGE          => s"Invalidated due to nomenclature code changes"
      case INVALIDATED_EU_MEASURE           => s"Invalidated due to EU measure"
      case INVALIDATED_NATIONAL_MEASURE     => s"Invalidated due to national legal measure"
      case INVALIDATED_WRONG_CLASSIFICATION => s"Invalidated due to incorrect classification"
      case INVALIDATED_OTHER | OTHER        => s"Invalidated due to other reasons"
      case unknown: CancelReason            =>
        throw new IllegalArgumentException(s"Unexpected reason: $unknown")
    }

    message + code(reason).map(c => s" ($c)").getOrElse("")
  }

  def code(reason: CancelReason): Option[Int] =
    reason match {
      case ANNULLED                         => Some(55)
      case INVALIDATED_CODE_CHANGE          => Some(61)
      case INVALIDATED_EU_MEASURE           => Some(62)
      case INVALIDATED_NATIONAL_MEASURE     => Some(63)
      case INVALIDATED_WRONG_CLASSIFICATION => Some(64)
      case INVALIDATED_OTHER                => Some(65)
      case OTHER                            => None
      case unknown: CancelReason            =>
        throw new IllegalArgumentException(s"Unexpected reason: $unknown")
    }

  implicit val format: Format[CancelReason] = Json.formatEnum(this)
}

case class Cancellation(
  reason: CancelReason.Value,
  applicationForExtendedUse: Boolean = false
)

object Cancellation {
  implicit val format: OFormat[Cancellation] = Json.format[Cancellation]
}

object AppealType extends Enumeration {
  def format(value: AppealType): String = value match {
    case ADR              => "Alternative Dispute Resolution (ADR)"
    case REVIEW           => "Review"
    case APPEAL_TIER_1    => "Appeal tier 1"
    case APPEAL_TIER_2    => "Appeal tier 2"
    case COURT_OF_APPEALS => "Court of appeals"
    case SUPREME_COURT    => "Supreme Court"
  }

  def heading(value: AppealType): String = value match {
    case ADR    => "Dispute"
    case REVIEW => "Review"
    case _      => "Appeal"
  }

  /** The order of enum matters as it is used how to show elements in UI in some cases, where it is
    * sorted by ID
    */
  type AppealType = Value
  val ADR, REVIEW, APPEAL_TIER_1, APPEAL_TIER_2, COURT_OF_APPEALS, SUPREME_COURT = Value

  implicit val format: Format[AppealType.Value] = Json.formatEnum(this)
}

object AppealStatus extends Enumeration {
  type AppealStatus = Value
  val IN_PROGRESS, ALLOWED, DISMISSED = Value

  def format(`type`: AppealType.Value, status: AppealStatus): String = `type` match {
    case AppealType.ADR    => formatDispute(status)
    case AppealType.REVIEW => formatReview(status)
    case _                 => formatAppeal(status)
  }

  def formatAppeal(status: AppealStatus): String = status match {
    case IN_PROGRESS => "Under appeal"
    case ALLOWED     => "Appeal allowed"
    case DISMISSED   => "Appeal dismissed"
  }

  def formatReview(status: AppealStatus): String = status match {
    case IN_PROGRESS => "Under review"
    case ALLOWED     => "Review upheld"
    case DISMISSED   => "Review overturned"
  }

  def formatDispute(status: AppealStatus): String = status match {
    case IN_PROGRESS => "Under mediation"
    case ALLOWED     => "Completed"
    case DISMISSED   => "Completed"
  }

  def validFor(appealType: AppealType.Value): Seq[AppealStatus] = appealType match {
    case AppealType.REVIEW =>
      Seq(AppealStatus.IN_PROGRESS, AppealStatus.ALLOWED, AppealStatus.DISMISSED)
    case AppealType.ADR    => Seq(AppealStatus.IN_PROGRESS, AppealStatus.ALLOWED)
    case _                 => Seq(AppealStatus.IN_PROGRESS, AppealStatus.ALLOWED, AppealStatus.DISMISSED)
  }

  implicit val format: Format[AppealStatus] = Json.formatEnum(this)

}

case class Appeal(
  id: String,
  status: AppealStatus.Value,
  `type`: AppealType.Value
)

object Appeal {

  def highestAppealFromDecision(decision: Option[Decision]): Option[Appeal] = {
    val appeals = decision.map(_.appeal).getOrElse(Seq.empty)

    if (appeals.nonEmpty) {
      Some(appeals.maxBy(_.`type`.id))
    } else {
      None
    }
  }

  implicit val fmt: OFormat[Appeal] = Json.format[Appeal]
}

case class Decision(
  bindingCommodityCode: String,
  effectiveStartDate: Option[Instant] = None,
  effectiveEndDate: Option[Instant] = None,
  justification: String,
  goodsDescription: String,
  methodSearch: Option[String] = None,
  methodExclusion: Option[String] = None,
  methodCommercialDenomination: Option[String] = None,
  appeal: Seq[Appeal] = Seq.empty,
  cancellation: Option[Cancellation] = None,
  explanation: Option[String] = None,
  decisionPdf: Option[Attachment] = None,
  letterPdf: Option[Attachment] = None
)
object Decision {
  implicit val fmt: OFormat[Decision] = Json.format[Decision]
}

object CaseStatus extends Enumeration {
  type CaseStatus = Value
  val DRAFT, NEW, OPEN, SUPPRESSED, REFERRED, REJECTED, CANCELLED, SUSPENDED, COMPLETED, REVOKED,
    ANNULLED = Value

  val openStatuses: Set[Value] = Set(OPEN, REFERRED, SUSPENDED)

  def formatCancellation(cse: ValuationCase) = cse.status match {
    case CaseStatus.CANCELLED =>
      val cancellationCode = cse.decision
        .flatMap(_.cancellation)
        .flatMap(c => CancelReason.code(c.reason))
        .map(c => s" - $c")
        .getOrElse("")

      cse.status.toString + cancellationCode

    case _ =>
      cse.status.toString
  }

  implicit val format: Format[CaseStatus] = Json.formatEnum(this)
}

object Role extends Enumeration {
  type Role = Value
  val CLASSIFICATION_OFFICER, CLASSIFICATION_MANAGER, READ_ONLY = Value

  def format(roleType: Role): String =
    roleType match {
      case CLASSIFICATION_OFFICER => "Classification officer"
      case CLASSIFICATION_MANAGER => "Manager"
      case READ_ONLY              => "Unknown"

    }
  implicit val format: Format[Role] = Json.formatEnum(this)
}

case class CaseWorker(
  id: String,
  name: Option[String] = None,
  email: Option[String] = None,
  role: Role.Role = Role.CLASSIFICATION_OFFICER
)
object CaseWorker {
  implicit val format: OFormat[CaseWorker] = Json.format[CaseWorker]
}

case class Attachment(
  id: String,
  public: Boolean = false,
  operator: Option[CaseWorker],
  timestamp: Instant = Instant.now(),
  description: Option[String] = None,
  shouldPublishToRulings: Boolean = false
)

object Attachment {
  implicit val format: OFormat[Attachment] = Json.format[Attachment]
}

case class AgentDetails(
  eoriDetails: EORIDetails,
  letterOfAuthorisation: Option[Attachment]
)

object AgentDetails {
  implicit val format: OFormat[AgentDetails] = Json.format[AgentDetails]
}

case class EORIDetails(
  eori: String,
  businessName: String,
  addressLine1: String,
  addressLine2: String,
  addressLine3: String,
  postcode: String,
  country: String
)

object EORIDetails {
  implicit val format: OFormat[EORIDetails] = Json.format[EORIDetails]
}

case class Contact(
  name: String,
  email: String,
  phone: Option[String] = None
)

object Contact {
  implicit val format: OFormat[Contact] = Json.format[Contact]
}

case class ValuationApplication(
  holder: EORIDetails,
  contact: Contact,
  goodName: String,
  goodDescription: String,
  agent: Option[AgentDetails] = None,
  confidentialInformation: Option[String] = None,
  otherInformation: Option[String] = None,
  knownLegalProceedings: Option[String] = None,
  envisagedCommodityCode: Option[String] = None,
  applicationPdf: Option[Attachment] = None
)

object ValuationApplication {
  implicit val format: OFormat[ValuationApplication] = Json.format[ValuationApplication]
}

case class ValuationCase(
  reference: String,
  status: CaseStatus.Value,
  createdDate: Instant,
  daysElapsed: Long,
  application: ValuationApplication,
  referredDaysElapsed: Long,
  caseBoardsFileNumber: Option[String] = None,
  assignee: Option[CaseWorker] = None,
  decision: Option[Decision] = None,
  attachments: Seq[Attachment] = Seq.empty,
  keywords: Set[String] = Set.empty,
  dateOfExtract: Option[Instant] = None,
  migratedDaysElapsed: Option[Long] = None
)

object ValuationCase {
  implicit val fmt: OFormat[ValuationCase] = Json.format[ValuationCase]
}
