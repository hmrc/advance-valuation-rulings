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

package uk.gov.hmrc.advancevaluationrulings.models

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsString, JsSuccess, Json}
import play.api.mvc.PathBindable
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class DraftIdSpec extends SpecBase {

  ".pathBindable" - {

    val pathBindable = implicitly[PathBindable[DraftId]]

    "must bind from a url" in {

      forAll(arbitrary[String], draftIdGen) { (key, value) =>
        pathBindable.bind(key, value.toString).value mustEqual value
      }
    }

    "must bind invalid value from a url" in {

      forAll(arbitrary[String], arbitrary[String]) { (key, value) =>
        whenever(!value.matches("DRAFT\\d{9}")) {
          pathBindable.bind(key, value).left.value mustEqual "Invalid draft Id"
        }
      }
    }

    "must unbind to a url" in {

      forAll(arbitrary[String], draftIdGen) { (key, value) =>
        pathBindable.unbind(key, value) mustEqual value.toString
      }
    }
  }

  ".format" - {

    "must serialise and deserialise to / from JSON" in {

      forAll(draftIdGen) { draftId =>
        val json = Json.toJson(draftId)
        json mustEqual JsString(draftId.toString)
        json.validate[DraftId] mustEqual JsSuccess(draftId)
      }
    }
  }

  ".fromString" - {

    "must return a DraftId when the string is valid" in {

      forAll(draftIdGen) { draftId =>
        DraftId.fromString(draftId.toString).value mustEqual draftId
      }
    }

    "must return None when the string is invalid" in {

      forAll(arbitrary[String]) { string =>
        whenever(!string.matches("DRAFT\\d{9}")) {
          DraftId.fromString(string) mustBe None
        }
      }
    }
  }
}
