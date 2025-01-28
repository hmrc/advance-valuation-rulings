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

package uk.gov.hmrc.advancevaluationrulings.models.audit

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application.*
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.Instant
import java.time.temporal.ChronoUnit

class ApplicationSubmissionEventSpec extends SpecBase {

  private val trader       =
    TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(true))
  private val goodsDetails = GoodsDetails("description", None, None, None, None, None)
  private val method       = MethodOne(None, None, None)
  private val contact      = ContactDetails("name", "email", None, None, None)
  private val now          = Instant.now.truncatedTo(ChronoUnit.MILLIS)

  private val application = Application(
    id = ApplicationId(1),
    applicantEori = "applicantEori",
    trader = trader,
    agent = None,
    contact = contact,
    goodsDetails = goodsDetails,
    requestedMethod = method,
    attachments = Nil,
    whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
    letterOfAuthority = None,
    submissionReference = "submissionReference",
    created = now,
    lastUpdated = now
  )

  private val event = ApplicationSubmissionEvent("internalId", AffinityGroup.Individual, None, application, DraftId(1))

  "An ApplicationSubmissionEvent" - {

    "must serialize and deserialize to/from JSON" in {
      val json = Json.toJson(event)
      json.validate[ApplicationSubmissionEvent] mustEqual JsSuccess(event)
    }

    "must fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj("invalid" -> "data")
      invalidJson.validate[ApplicationSubmissionEvent].isError mustBe true
    }

    "must have a working equals and hashCode" in {
      event mustEqual event
      event.hashCode mustEqual event.hashCode
    }

    "must have a working toString" in {
      event.toString must include("ApplicationSubmissionEvent")
    }
  }
}
