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

package uk.gov.hmrc.advancevaluationrulings.models.application

import play.api.libs.json.{Json, OFormat}

final case class TraderDetail(
                               eori: String,
                               businessName: String,
                               addressLine1: String,
                               addressLine2: Option[String],
                               addressLine3: Option[String],
                               postcode: String,
                               countryCode: String,
                               phoneNumber: Option[String]
                             ) {

  lazy val country: Country = Country.fromCountryCode(countryCode)

  lazy val addressLines: Seq[String] = Seq(
    Some(addressLine1),
    addressLine2,
    addressLine3,
    Some(postcode),
    Some(country.name)
  ).flatten
}

object TraderDetail {

  implicit lazy val format: OFormat[TraderDetail] = Json.format
}
