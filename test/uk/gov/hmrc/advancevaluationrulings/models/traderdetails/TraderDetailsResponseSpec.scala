/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.models.traderdetails

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.models.etmp.CDSEstablishmentAddress

class TraderDetailsResponseSpec extends AnyFreeSpec with Matchers with EitherValues {

  val establishmentAddress: CDSEstablishmentAddress = CDSEstablishmentAddress(
    streetAndNumber = "123 Example Street",
    city = "Example City",
    postalCode = Some("EX4 3PL"),
    countryCode = "GB"
  )

  val traderDetailsResponse: TraderDetailsResponse = TraderDetailsResponse(
    EORINo = "GB1234567890",
    CDSFullName = "Trader Name",
    CDSEstablishmentAddress = establishmentAddress,
    consentToDisclosureOfPersonalData = true,
    contactInformation = None
  )

  "A TraderDetailsResponse" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(traderDetailsResponse)
      json.validate[TraderDetailsResponse] mustEqual JsSuccess(traderDetailsResponse)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[TraderDetailsResponse].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      traderDetailsResponse mustEqual traderDetailsResponse
      traderDetailsResponse.hashCode mustEqual traderDetailsResponse.hashCode
    }

    "must have a working toString" in {
      traderDetailsResponse.toString must include("TraderDetailsResponse")
    }
  }
}
