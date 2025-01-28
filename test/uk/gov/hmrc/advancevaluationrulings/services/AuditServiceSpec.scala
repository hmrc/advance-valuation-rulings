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

package uk.gov.hmrc.advancevaluationrulings.services

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import play.api
import play.api.inject.bind
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.DraftId
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.models.audit.ApplicationSubmissionEvent
import uk.gov.hmrc.auth.core.{AffinityGroup, Assistant}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.Instant
import java.time.temporal.ChronoUnit

class AuditServiceSpec extends AnyFreeSpec with SpecBase with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector)
  }

  private val mockAuditConnector = mock(classOf[AuditConnector])

  private lazy val app: api.Application = applicationBuilder
    .overrides(
      bind[AuditConnector].toInstance(mockAuditConnector)
    )
    .build()

  private lazy val service: AuditService = app.injector.instanceOf[AuditService]

  private val hc: HeaderCarrier = HeaderCarrier()

  private val trader       =
    TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(false))
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

  "auditSubmitRequest" - {

    "must call the audit connector with the right values" in {

      val event = ApplicationSubmissionEvent(
        internalId = "internalId",
        affinityGroup = AffinityGroup.Organisation,
        credentialRole = Some(Assistant),
        application = application,
        draftId = DraftId(1)
      )

      service.auditSubmitRequest(event)(hc)

      verify(mockAuditConnector).sendExplicitAudit(eqTo("ApplicationSubmission"), eqTo(event))(
        eqTo(hc),
        any(),
        any()
      )
    }
  }
}
