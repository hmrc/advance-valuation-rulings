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

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

import java.time.{LocalDateTime, ZoneOffset}

class UserAnswersSpec extends SpecBase {

  val userAnswersJson: JsObject =
    Json.obj(
      "userId"      -> "some_fake_id",
      "draftId"     -> "DRAFT000000001",
      "data"        -> Json.obj("some_json_field" -> "some_json_data"),
      "lastUpdated" -> "2024-01-01T00:00:00Z"
    )

  val model: UserAnswers =
    UserAnswers(
      "some_fake_id",
      DraftId(1),
      Json.obj("some_json_field" -> Json.toJson("some_json_data")),
      lastUpdated = LocalDateTime
        .of(2024, 1, 1, 0, 0, 0)
        .toInstant(ZoneOffset.UTC)
    )

  "UserAnswers" - {

    "reads" - {

      "must read to correct model" in {

        val actual = userAnswersJson.as[UserAnswers]

        val expected = model

        actual mustBe expected
      }
    }

    "writes" - {

      "must write to correct json" in {

        val actual = Json.toJson(model)

        val expected = userAnswersJson

        actual mustBe expected
      }
    }
  }
}
