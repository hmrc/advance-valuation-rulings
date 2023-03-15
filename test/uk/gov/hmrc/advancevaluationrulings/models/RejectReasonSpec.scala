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

import play.api.libs.json.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RejectReasonSpec extends AnyWordSpec with Matchers {
  "json serialization" should {
    "form an isomorphism for Application Withdrawn" in {
      val reason: RejectReason = RejectReason.APPLICATION_WITHDRAWN

      val formatted = Json.toJson(reason)
      val result    = Json.fromJson[RejectReason](formatted)

      result.get shouldBe reason
    }

    "form an isomorphism for ATaR ruling already exists" in {
      val reason: RejectReason = RejectReason.ATAR_RULING_ALREADY_EXISTS

      val formatted = Json.toJson(reason)
      val result    = Json.fromJson[RejectReason](formatted)

      result.get shouldBe reason
    }

    "form an isomorphism for Duplicate application" in {
      val reason: RejectReason = RejectReason.DUPLICATE_APPLICATION

      val formatted = Json.toJson(reason)
      val result    = Json.fromJson[RejectReason](formatted)

      result.get shouldBe reason
    }

    "form an isomorphism for No information from trader" in {
      val reason: RejectReason = RejectReason.NO_INFO_FROM_TRADER

      val formatted = Json.toJson(reason)
      val result    = Json.fromJson[RejectReason](formatted)

      result.get shouldBe reason
    }

    "form an isomophism for Other" in {
      val reason: RejectReason = RejectReason.OTHER

      val formatted = Json.toJson(reason)
      val result    = Json.fromJson[RejectReason](formatted)

      result.get shouldBe reason
    }

  }

}
