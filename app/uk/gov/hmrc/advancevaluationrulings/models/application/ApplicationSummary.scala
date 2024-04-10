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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class ApplicationSummary(
  id: ApplicationId,
  goodsDescription: String,
  dateSubmitted: Instant,
  eoriNumber: String
)

object ApplicationSummary {

  val mongoReads: Reads[ApplicationSummary] =
    (
      (__ \ "id").read[ApplicationId] and
        (__ \ "goodsDetails" \ "goodsDescription").read[String] and
        (__ \ "created").read[Instant](MongoJavatimeFormats.instantFormat) and
        (__ \ "trader" \ "eori").read[String]
    )(ApplicationSummary.apply _)

  val mongoFormat: OFormat[ApplicationSummary] = OFormat(mongoReads, format)

  implicit lazy val format: OFormat[ApplicationSummary] = Json.format
}
