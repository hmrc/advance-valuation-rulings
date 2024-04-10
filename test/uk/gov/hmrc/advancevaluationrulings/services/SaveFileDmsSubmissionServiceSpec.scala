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
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.file.{Files, Paths}
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class SaveFileDmsSubmissionServiceSpec extends SpecBase with BeforeAndAfterEach {

  val fileName: String = "applications/" + "application.pdf"

  override def beforeEach(): Unit = {
    super.beforeEach()

    reset(mockFopService)

    Paths.get(fileName).toFile.delete()
  }

  private val mockFopService = mock[FopService]

  private lazy val app = applicationBuilder
    .overrides(
      bind[FopService].toInstance(mockFopService)
    )
    .configure(
      "dms-submission.enabled" -> false
    )
    .build()

  private lazy val service                     = app.injector.instanceOf[DmsSubmissionService]
  private lazy val applicationTemplate         = app.injector.instanceOf[ApplicationPdf]
  private implicit lazy val messages: Messages =
    app.injector.instanceOf[MessagesApi].preferred(Seq.empty)

  "submitApplication" - {

    val trader              =
      TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(false))
    val goodsDetails        = GoodsDetails("description", None, None, None, None, None)
    val method              = MethodOne(None, None, None)
    val contact             = ContactDetails("name", "email", None, Some("Bob Inc"), Some("CEO"))
    val submissionReference = "submissionReference"
    val now                 = Instant.now.truncatedTo(ChronoUnit.MILLIS)

    val attachment = Attachment(
      id = 1,
      name = "attachment",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1337
    )

    val letterOfAuthority = Attachment(
      id = 2,
      name = "loa",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1323
    )

    val application = Application(
      id = ApplicationId(1),
      applicantEori = "applicantEori",
      trader = trader,
      agent = None,
      contact = contact,
      goodsDetails = goodsDetails,
      requestedMethod = method,
      attachments = Seq(attachment),
      whatIsYourRoleResponse = Some(WhatIsYourRole.EmployeeOrg),
      letterOfAuthority = Some(letterOfAuthority),
      submissionReference = submissionReference,
      created = now,
      lastUpdated = now
    )

    val hc: HeaderCarrier = HeaderCarrier()

    "must create a PDF from the application and save it on disk" in {

      val bytes = "Hello, World!".getBytes("UTF-8")

      when(mockFopService.render(any())).thenReturn(Future.successful(bytes))

      val expectedXml = applicationTemplate(application).body

      service.submitApplication(application, submissionReference)(hc).futureValue

      verify(mockFopService).render(eqTo(expectedXml))

      val result = Files.readAllBytes(Paths.get(fileName))

      result mustEqual bytes
    }

    "must fail if the fop service fails" in {

      when(mockFopService.render(any())).thenReturn(Future.failed(new RuntimeException()))

      service.submitApplication(application, submissionReference)(hc).failed.futureValue

    }

  }
}
