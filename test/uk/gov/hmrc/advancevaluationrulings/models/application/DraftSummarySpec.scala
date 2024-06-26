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
import play.api.libs.json.Json
import uk.gov.hmrc.advancevaluationrulings.models.{DraftId, UserAnswers}

import java.time.Instant

class DraftSummarySpec extends AnyFreeSpec with Matchers {

  private val now = Instant.now

  ".apply" - {

    "must return a DraftSummary when optional fields have not been answered" in {

      val draftId = DraftId(0)
      val answers = UserAnswers("userId", draftId, Json.obj(), now)

      DraftSummary(answers) mustEqual DraftSummary(draftId, None, now, None)
    }

    "must return a DraftSummary when optional fields have been answered" in {

      val draftId = DraftId(0)
      val data    = Json.obj(
        "goodsDescription"       -> "goods",
        "checkRegisteredDetails" -> Json.obj(
          "eori" -> "eori"
        )
      )

      val answers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustEqual DraftSummary(draftId, Some("goods"), now, Some("eori"))
    }

    "must return a DraftSummary when optional field 'goodsDescription' and 'checkRegisteredDetails' have not been answered" in {

      val draftId = DraftId(0)
      val data    = Json.obj()

      val answers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustEqual DraftSummary(draftId, None, now, None)
    }

    "must return a DraftSummary when optional field 'goodsDescription' and 'checkRegisteredDetails' have been answered as int" in {

      val draftId = DraftId(0)
      val data    = Json.obj(
        "goodsDescription"       -> 1,
        "checkRegisteredDetails" -> 2
      )
      val answers = UserAnswers("userId", draftId, data, now)

      DraftSummary(answers) mustEqual DraftSummary(draftId, None, now, None)
    }
  }
}
