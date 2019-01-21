/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.model

import play.api.libs.json._
import uk.gov.hmrc.play.json.Union

object RESTFormatters {


  // `Case` formatters
  implicit val formatCaseStatus = EnumJson.format(CaseStatus)
  implicit val formatApplicationType = EnumJson.format(ApplicationType)
  implicit val formatLiabilityStatus = EnumJson.format(LiabilityStatus)

  implicit val formatOperator = Json.format[Operator]
  implicit val formatEORIDetails = Json.format[EORIDetails]
  implicit val formatAttachment = Json.format[Attachment]
  implicit val formatAgentDetails = Json.format[AgentDetails]
  implicit val formatContact = Json.format[Contact]

  implicit val formatLiabilityOrder = Json.format[LiabilityOrder]
  implicit val formatBTIApplication = Json.format[BTIApplication]
  implicit val formatApplication = Union.from[Application]("type")
    .and[BTIApplication](ApplicationType.BTI.toString)
    .and[LiabilityOrder](ApplicationType.LIABILITY_ORDER.toString)
    .format

  implicit val formatAppeal = Json.format[Appeal]
  implicit val formatDecision = Json.format[Decision]

  implicit val formatCase = Json.format[Case]
  implicit val formatNewCase = Json.format[NewCaseRequest]

  implicit val formatStatus = Json.format[Status]

  // `Event` formatters
  implicit val formatCaseStatusChange = Json.format[CaseStatusChange]
  implicit val formatNote = Json.format[Note]

  implicit val formatEventDetail = Union.from[Details]("type")
    .and[CaseStatusChange](EventType.CASE_STATUS_CHANGE.toString)
    .and[Note](EventType.NOTE.toString)
    .format


  implicit val formatEvent = Json.format[Event]
  implicit val formatNewEventRequest = Json.format[NewEventRequest]

  implicit val formatBankHoliday = Json.format[BankHoliday]
  implicit val formatBankHolidaysSet = Json.format[BankHolidaySet]
  implicit val formatBankHolidaysResponse = Json.format[BankHolidaysResponse]
}
