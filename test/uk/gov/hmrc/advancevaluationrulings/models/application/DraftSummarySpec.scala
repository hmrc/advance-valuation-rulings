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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.advancevaluationrulings.models.{DraftId, UserAnswers}

import java.time.Instant

class DraftSummarySpec extends AnyFreeSpec with Matchers {

  private val now: Instant     = Instant.now
  private val draftId: DraftId = DraftId(0)

  ".apply" - {

    "must return a DraftSummary when optional fields 'goodsDescription' and 'eori' have not been answered" in {

      val answers: UserAnswers = UserAnswers("userId", draftId, Json.obj(), now)

      DraftSummary(answers) mustBe DraftSummary(draftId, None, now, None)
    }

    "must return a DraftSummary when optional fields have been answered correctly" in {

      val data: JsObject = Json.obj(
        "goodsDescription"       -> "goods",
        "checkRegisteredDetails" -> Json.obj(
          "eori" -> "eori"
        )
      )

      val answers: UserAnswers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustBe DraftSummary(draftId, Some("goods"), now, Some("eori"))
    }

    "must return a DraftSummary when optional field 'goodsDescription' and 'checkRegisteredDetails' object have been answered as int" in {

      val data: JsObject = Json.obj(
        "goodsDescription"       -> 1,
        "checkRegisteredDetails" -> 2
      )

      val answers: UserAnswers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustBe DraftSummary(draftId, None, now, None)
    }

    "must return a DraftSummary when optional fields 'goodsDescription' and 'eori' have been answered as int" in {

      val data: JsObject = Json.obj(
        "goodsDescription"       -> 1,
        "checkRegisteredDetails" -> Json.obj(
          "eori" -> 2
        )
      )

      val answers: UserAnswers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustBe DraftSummary(draftId, None, now, None)
    }
  }
}
