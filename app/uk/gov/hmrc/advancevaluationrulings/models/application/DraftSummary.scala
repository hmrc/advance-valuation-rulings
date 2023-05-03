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

package uk.gov.hmrc.advancevaluationrulings.models.application

import java.time.Instant

import play.api.libs.json._
import uk.gov.hmrc.advancevaluationrulings.models.{DraftId, UserAnswers}

final case class DraftSummary(
  id: DraftId,
  goodsName: Option[String],
  lastUpdated: Instant,
  eoriNumber: Option[String]
)

object DraftSummary {

  implicit lazy val format: OFormat[DraftSummary] = Json.format

  def apply(answers: UserAnswers): DraftSummary =
    DraftSummary(
      id = answers.draftId,
      goodsName = Reads
        .optionNoError(Reads.at[String](JsPath \ "goodsDescription"))
        .reads(answers.data)
        .getOrElse(None),
      lastUpdated = answers.lastUpdated,
      eoriNumber = Reads
        .optionNoError(Reads.at[String](JsPath \ "checkRegisteredDetails" \ "eori"))
        .reads(answers.data)
        .getOrElse(None)
    )
}
