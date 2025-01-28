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

package uk.gov.hmrc.advancevaluationrulings.models.application

import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsString, JsSuccess, Json}
import play.api.mvc.PathBindable
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class ApplicationIdSpec extends SpecBase {

  "an application Id" - {

    val pathBindable = implicitly[PathBindable[ApplicationId]]

    "must bind from a url" in {

      forAll(arbitrary[String], applicationIdGen) { (key, value) =>
        pathBindable.bind(key, value.toString).value mustEqual value
      }
    }

    "must fail to bind invalid applicationId" in {
      pathBindable.bind("value", "INVALID").left.value mustEqual "Invalid application Id"
    }

    "must unbind to a url" in {

      forAll(arbitrary[String], applicationIdGen) { (key, value) =>
        pathBindable.unbind(key, value) mustEqual value.toString
      }
    }

    "must serialise and deserialise to / from JSON" in {

      forAll(applicationIdGen) { applicationId =>
        val json = Json.toJson(applicationId)
        json mustEqual JsString(applicationId.toString)
        json.validate[ApplicationId] mustEqual JsSuccess(applicationId)
      }
    }
  }
}
