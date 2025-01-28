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

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

class AttachmentRequestSpec extends AnyFreeSpec with Matchers with EitherValues {

  val attachmentRequest: AttachmentRequest = AttachmentRequest(
    name = "example.pdf",
    description = Some("An example PDF document"),
    url = "s3://bucket/example.pdf",
    privacy = Privacy.Public,
    mimeType = "application/pdf",
    size = 1024L
  )

  "An AttachmentRequest" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(attachmentRequest)
      json.validate[AttachmentRequest] mustEqual JsSuccess(attachmentRequest)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[AttachmentRequest].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      attachmentRequest mustEqual attachmentRequest
      attachmentRequest.hashCode mustEqual attachmentRequest.hashCode
    }

    "must have a working toString" in {
      attachmentRequest.toString must include("AttachmentRequest")
    }
  }
}
