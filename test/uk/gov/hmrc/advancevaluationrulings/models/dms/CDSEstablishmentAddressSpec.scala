/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.advancevaluationrulings.models.dms

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.etmp.CDSEstablishmentAddress

class CDSEstablishmentAddressSpec extends SpecBase {

  val address: CDSEstablishmentAddress = CDSEstablishmentAddress(
    streetAndNumber = "123 Example Street",
    city = "Example City",
    countryCode = "EX",
    postalCode = Some("12345")
  )

  "A CDSEstablishmentAddress" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(address)
      json.validate[CDSEstablishmentAddress] mustEqual JsSuccess(address)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[CDSEstablishmentAddress].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      address mustEqual address
      address.hashCode mustEqual address.hashCode
    }

    "must have a working toString" in {
      address.toString must include("CDSEstablishmentAddress")
    }
  }
}
