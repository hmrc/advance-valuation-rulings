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

package uk.gov.hmrc.advancevaluationrulings.models.application

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsString, JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class PrivacySpec extends SpecBase {

  "Privacy" - {

    "must serialize and deserialize to/from JSON" in {
      val cases = Seq(
        Privacy.Public       -> "Public",
        Privacy.HmrcOnly     -> "HmrcOnly",
        Privacy.Confidential -> "Confidential"
      )

      cases.foreach { case (privacy, string) =>
        val json = Json.toJson(privacy)
        json mustEqual JsString(string)
        json.validate[Privacy] mustEqual JsSuccess(privacy)
      }
    }

    "must have the correct enum values" in {
      Privacy.values must contain theSameElementsAs Seq(Privacy.Public, Privacy.HmrcOnly, Privacy.Confidential)
    }
  }
}
