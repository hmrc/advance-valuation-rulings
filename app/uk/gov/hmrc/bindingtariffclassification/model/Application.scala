/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.Instant

import uk.gov.hmrc.bindingtariffclassification.model
import uk.gov.hmrc.bindingtariffclassification.model.ApplicationType.ApplicationType
import uk.gov.hmrc.bindingtariffclassification.model.ImportExport.ImportExport
import uk.gov.hmrc.bindingtariffclassification.model.LiabilityStatus.LiabilityStatus

sealed trait Application {
  val `type`: ApplicationType
  val contact: Contact

  def isBTI: Boolean = isInstanceOf[BTIApplication]
  def isLiabilityOrder: Boolean = isInstanceOf[LiabilityOrder]
  def asBTI: BTIApplication = asInstanceOf[BTIApplication]
  def asLiabilityOrder: LiabilityOrder = asInstanceOf[LiabilityOrder]
}

case class BTIApplication
(
  holder: EORIDetails,
  override val contact: Contact,
  agent: Option[AgentDetails] = None,
  offline: Boolean = false,
  goodName: String,
  goodDescription: String,
  confidentialInformation: Option[String] = None,
  importOrExport: Option[ImportExport] = None,
  otherInformation: Option[String] = None,
  reissuedBTIReference: Option[String] = None,
  relatedBTIReference: Option[String] = None,
  knownLegalProceedings: Option[String] = None,
  envisagedCommodityCode: Option[String] = None,
  sampleToBeProvided: Boolean = false,
  sampleToBeReturned: Boolean = false
) extends Application {
  override val `type`: model.ApplicationType.Value = ApplicationType.BTI
}

case class LiabilityOrder
(
  override val contact: Contact,
  goodName: Option[String],
  status: LiabilityStatus,
  traderName: String,
  entryDate: Option[Instant] = None,
  entryNumber: Option[String] = None,
  traderCommodityCode: Option[String] = None,
  officerCommodityCode: Option[String] = None,
  btiReference: Option[String] = None,
  repaymentClaim: Option[RepaymentClaim] = None,
  dateOfReceipt: Option[Instant] = None,
  traderContactDetails: Option[TraderContactDetails] = None
) extends Application {
  override val `type`: model.ApplicationType.Value = ApplicationType.LIABILITY_ORDER
}

case class EORIDetails
(
  eori: String,
  businessName: String,
  addressLine1: String,
  addressLine2: String,
  addressLine3: String,
  postcode: String,
  country: String
)

case class AgentDetails
(
  eoriDetails: EORIDetails,
  letterOfAuthorisation: Option[Attachment]
)

case class Contact
(
  name: String,
  email: String,
  phone: Option[String]
)

object LiabilityStatus extends Enumeration {
  type LiabilityStatus = Value
  val LIVE, NON_LIVE = Value
}

object ApplicationType extends Enumeration {
  type ApplicationType = Value
  val BTI, LIABILITY_ORDER = Value
}

object ImportExport extends Enumeration {
  type ImportExport = Value
  val IMPORT, EXPORT = Value
}
