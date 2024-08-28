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
import play.api.{Application => FakeApplication}
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.file.attribute.PosixFilePermission
import java.nio.file.{Files, Path, Paths}
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util
import scala.concurrent.Future

class SaveFileDmsSubmissionServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val pdfFileName: String = "applications/application.pdf"
  private val pdfFilePath: Path   = Paths.get(pdfFileName)

  private val mockFopService: FopService = mock(classOf[FopService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFopService)
    pdfFilePath.toFile.delete()
  }

  private lazy val app: FakeApplication = applicationBuilder
    .overrides(
      bind[FopService].toInstance(mockFopService)
    )
    .configure(
      "dms-submission.enabled" -> false
    )
    .build()

  private lazy val service: DmsSubmissionService       = app.injector.instanceOf[DmsSubmissionService]
  private lazy val applicationTemplate: ApplicationPdf = app.injector.instanceOf[ApplicationPdf]
  private implicit lazy val messages: Messages         =
    app.injector.instanceOf[MessagesApi].preferred(Seq.empty)

  "submitApplication" - {

    val trader: TraderDetail        =
      TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None, Some(false))
    val goodsDetails: GoodsDetails  = GoodsDetails("description", None, None, None, None, None)
    val method: MethodOne           = MethodOne(None, None, None)
    val contact: ContactDetails     = ContactDetails("name", "email", None, Some("Bob Inc"), Some("CEO"))
    val submissionReference: String = "submissionReference"
    val now: Instant                = Instant.now.truncatedTo(ChronoUnit.MILLIS)

    val attachment: Attachment = Attachment(
      id = 1,
      name = "attachment",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1337
    )

    val letterOfAuthority: Attachment = Attachment(
      id = 2,
      name = "loa",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1323
    )

    val application: Application = Application(
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

    val bytes: Array[Byte] = "Hello, World!".getBytes("UTF-8")

    val hc: HeaderCarrier = HeaderCarrier()

    "must create a PDF from the application and save it on disk" in {

      when(mockFopService.render(any())).thenReturn(Future.successful(bytes))

      val expectedXml: String = applicationTemplate(application).body

      service.submitApplication(application, submissionReference)(hc).futureValue

      verify(mockFopService).render(eqTo(expectedXml))

      val result: Array[Byte] = Files.readAllBytes(pdfFilePath)

      result mustBe bytes
    }

    "must fail if the fop service fails" in {

      when(mockFopService.render(any())).thenReturn(Future.failed(new RuntimeException()))

      val result: Throwable = service.submitApplication(application, submissionReference)(hc).failed.futureValue

      result mustBe a[RuntimeException]
    }

    "must return Done to handle IOException when file writing fails" in {

      when(mockFopService.render(any())).thenReturn(Future.successful(bytes))

      Files.write(pdfFilePath, bytes)
      Files.setPosixFilePermissions(pdfFilePath, util.Set.of(PosixFilePermission.OWNER_READ))

      val result: Done = service.submitApplication(application, submissionReference)(hc).futureValue

      result mustBe Done
    }
  }
}
