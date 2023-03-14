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

import java.time.Instant

import play.api.libs.json._
import uk.gov.hmrc.advancevaluationrulings.models.common.UserAnswers
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

final case class ValuationRulingsApplication(
  data: UserAnswers,
  applicationNumber: String,
  lastUpdated: Instant
)

object ValuationRulingsApplication {

  val reads: Reads[ValuationRulingsApplication] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").read[UserAnswers] and
        (__ \ "applicationNumber").read[String] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
    )(ValuationRulingsApplication.apply _)
  }

  val writes: OWrites[ValuationRulingsApplication] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "data").write[UserAnswers] and
        (__ \ "applicationNumber").write[String] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
    )(unlift(ValuationRulingsApplication.unapply))
  }

  implicit val format: OFormat[ValuationRulingsApplication] = OFormat(reads, writes)
}
