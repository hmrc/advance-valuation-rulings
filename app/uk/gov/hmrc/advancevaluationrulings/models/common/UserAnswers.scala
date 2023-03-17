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

package uk.gov.hmrc.advancevaluationrulings.models.common

import play.api.libs.json.{Json, OFormat}

final case class UserAnswers(
  importGoods: Boolean,
  checkRegisteredDetails: RegisteredDetailsCheck,
  applicationContactDetails: ApplicationContactDetails,
  valuationMethod: ValuationMethod,
  isThereASaleInvolved: Option[Boolean],
  isSaleBetweenRelatedParties: Option[Boolean],
  areThereRestrictionsOnTheGoods: Option[Boolean],
  isTheSaleSubjectToConditions: Option[Boolean],
  whyIdenticalGoods: Option[String],
  adoptMethod: Option[String],
  valuationDescription: Option[String],
  haveYouUsedMethodOneInPast: Option[Boolean],
  describeTheIdenticalGoods: Option[String],
  descriptionOfGoods: String,
  hasCommodityCode: Boolean,
  commodityCode: Option[String],
  haveTheGoodsBeenSubjectToLegalChallenges: Boolean,
  hasConfidentialInformation: Boolean,
  doYouWantToUploadDocuments: Boolean,
  uploadSupportingDocument: Option[SupportingDocuments]
)

object UserAnswers {
  implicit val format: OFormat[UserAnswers] = Json.format[UserAnswers]
}
