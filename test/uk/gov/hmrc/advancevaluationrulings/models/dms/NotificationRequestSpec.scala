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

package uk.gov.hmrc.advancevaluationrulings.models.dms

import org.scalatest.freespec.AnyFreeSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase

class NotificationRequestSpec extends SpecBase {

  val notificationRequest: NotificationRequest = NotificationRequest(
    id = "12345",
    status = SubmissionItemStatus.Forwarded,
    failureReason = Some("Network error")
  )

  "A NotificationRequest" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(notificationRequest)
      json.validate[NotificationRequest] mustEqual JsSuccess(notificationRequest)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[NotificationRequest].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      notificationRequest mustEqual notificationRequest
      notificationRequest.hashCode mustEqual notificationRequest.hashCode
    }

    "must have a working toString" in {
      notificationRequest.toString must include("NotificationRequest")
    }
  }
}
