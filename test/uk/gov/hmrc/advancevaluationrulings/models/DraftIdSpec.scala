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

import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, JsSuccess, Json}
import play.api.mvc.PathBindable

class DraftIdSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with ModelGenerators with EitherValues {

  "an draft Id" - {

    val pathBindable = implicitly[PathBindable[DraftId]]

    "must bind from a url" in {

      forAll(arbitrary[String], draftIdGen) {
        (key, value) =>
          pathBindable.bind(key, value.toString).value mustEqual value
      }
    }

    "must unbind to a url" in {

      forAll(arbitrary[String], draftIdGen) {
        (key, value) =>
          pathBindable.unbind(key, value) mustEqual value.toString
      }
    }

    "must serialise and deserialise to / from JSON" in {

      forAll(draftIdGen) {
        draftId =>

          val json = Json.toJson(draftId)
          json mustEqual JsString(draftId.toString)
          json.validate[DraftId] mustEqual JsSuccess(draftId)
      }
    }
  }
}
