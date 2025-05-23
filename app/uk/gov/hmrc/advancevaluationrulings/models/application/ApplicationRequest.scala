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

package uk.gov.hmrc.advancevaluationrulings.models.application

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.advancevaluationrulings.models.DraftId

final case class ApplicationRequest(
  draftId: DraftId,
  trader: TraderDetail,
  agent: Option[TraderDetail],
  contact: ContactDetails,
  requestedMethod: RequestedMethod,
  goodsDetails: GoodsDetails,
  attachments: Seq[AttachmentRequest],
  whatIsYourRole: WhatIsYourRole,
  letterOfAuthority: Option[AttachmentRequest]
)

object ApplicationRequest {

  given format: OFormat[ApplicationRequest] = Json.format
}
