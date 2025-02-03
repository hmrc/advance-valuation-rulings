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

package uk.gov.hmrc.advancevaluationrulings.models.etmp

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class SubscriptionDisplayResponseSpec extends SpecBase {

  val responseCommon: ResponseCommon = ResponseCommon(
    status = "OK",
    statusText = Some("Success")
  )

  val responseDetail: Option[ResponseDetail] = Some(
    ResponseDetail(
      EORINo = "GB123456789000",
      CDSFullName = "Test Company",
      CDSEstablishmentAddress = CDSEstablishmentAddress(
        streetAndNumber = "123 Example Street",
        city = "Example City",
        countryCode = "EX",
        postalCode = Some("12345")
      ),
      contactInformation = None,
      consentToDisclosureOfPersonalData = None
    )
  )

  val subscriptionDisplayResponse: SubscriptionDisplayResponse = SubscriptionDisplayResponse(
    responseCommon = responseCommon,
    responseDetail = responseDetail
  )

  "A SubscriptionDisplayResponse" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(subscriptionDisplayResponse)
      json.validate[SubscriptionDisplayResponse] mustEqual JsSuccess(subscriptionDisplayResponse)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[SubscriptionDisplayResponse].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      subscriptionDisplayResponse mustEqual subscriptionDisplayResponse
      subscriptionDisplayResponse.hashCode mustEqual subscriptionDisplayResponse.hashCode
    }

    "must have a working toString" in {
      subscriptionDisplayResponse.toString must include("SubscriptionDisplayResponse")
    }
  }
}
