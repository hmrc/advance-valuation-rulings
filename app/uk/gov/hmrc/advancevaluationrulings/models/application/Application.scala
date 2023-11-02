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
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.advancevaluationrulings.models.DraftId

final case class Application(
  id: ApplicationId,
  draftId: DraftId,
  applicantEori: String,
  trader: TraderDetail,
  agent: Option[TraderDetail],
  contact: ContactDetails,
  requestedMethod: RequestedMethod,
  goodsDetails: GoodsDetails,
  attachments: Seq[Attachment],
  whatIsYourRoleResponse: Option[WhatIsYourRole],
  letterOfAuthority: Option[Attachment],
  submissionReference: String,
  created: Instant,
  lastUpdated: Instant
)

object Application {

  implicit def format(implicit f: Format[Instant]): OFormat[Application] = Json.format
}
