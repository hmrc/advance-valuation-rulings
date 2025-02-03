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

package uk.gov.hmrc.advancevaluationrulings.services

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import org.apache.pekko.util.ByteString
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.connectors.DmsSubmissionConnector
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class DefaultDmsSubmissionServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockFopService             = mock(classOf[FopService])
  private val mockDmsSubmissionConnector = mock(classOf[DmsSubmissionConnector])

  private lazy val app = applicationBuilder
    .overrides(
      bind[FopService].toInstance(mockFopService),
      bind[DmsSubmissionConnector].toInstance(mockDmsSubmissionConnector)
    )
    .configure(
      "dms-submission.enabled" -> true
    )
    .build()

  private lazy val service             = app.injector.instanceOf[DmsSubmissionService]
  private lazy val applicationTemplate = app.injector.instanceOf[ApplicationPdf]
  private given mat: Materializer      = app.injector.instanceOf[Materializer]
  private given messages: Messages     =
    app.injector.instanceOf[MessagesApi].preferred(Seq.empty)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFopService)
    reset(mockDmsSubmissionConnector)
  }

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

    "must create a PDF from the application and send it to DMS Submission" in {

      val bytes                                               = "Hello, World!".getBytes("UTF-8")
      val sourceCaptor: ArgumentCaptor[Source[ByteString, ?]] =
        ArgumentCaptor.forClass(classOf[Source[ByteString, ?]])

      when(mockFopService.render(any())).thenReturn(Future.successful(bytes))
      when(
        mockDmsSubmissionConnector.submitApplication(any(), any(), any(), any(), any(), any())(
          any()
        )
      )
        .thenReturn(Future.successful(Done))

      val expectedXml = applicationTemplate(application).body

      service.submitApplication(application, submissionReference)(hc).futureValue

      verify(mockFopService).render(eqTo(expectedXml))
      verify(mockDmsSubmissionConnector).submitApplication(
        eqTo("applicantEori"),
        sourceCaptor.capture(),
        eqTo(application.created),
        eqTo(submissionReference),
        eqTo(application.attachments),
        eqTo(application.letterOfAuthority)
      )(eqTo(hc))

      val result = sourceCaptor.getValue
        .toMat(Sink.fold(ByteString.emptyByteString)(_ ++ _))(Keep.right)
        .run()
        .futureValue

      result.decodeString("UTF-8") mustEqual "Hello, World!"
    }

    "must fail if the fop service fails" in {

      when(mockFopService.render(any())).thenReturn(Future.failed(new RuntimeException()))

      service.submitApplication(application, submissionReference)(hc).failed.futureValue

      verify(mockDmsSubmissionConnector, times(0))
        .submitApplication(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )(any())
    }

    "must fail if the dms submission connector fails" in {

      when(mockFopService.render(any())).thenReturn(Future.successful(Array.emptyByteArray))
      when(
        mockDmsSubmissionConnector.submitApplication(any(), any(), any(), any(), any(), any())(
          any()
        )
      )
        .thenReturn(Future.failed(new RuntimeException()))

      service.submitApplication(application, submissionReference)(hc).failed.futureValue
    }
  }
}
