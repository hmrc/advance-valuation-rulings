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

package uk.gov.hmrc.advancevaluationrulings.services

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.advancevaluationrulings.connectors.DmsSubmissionConnector
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.Future

class DmsSubmissionServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with OptionValues with MockitoSugar with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFopService, mockDmsSubmissionConnector)
  }

  private val mockFopService = mock[FopService]
  private val mockDmsSubmissionConnector = mock[DmsSubmissionConnector]

  private lazy val app = GuiceApplicationBuilder()
    .overrides(
      bind[FopService].toInstance(mockFopService),
      bind[DmsSubmissionConnector].toInstance(mockDmsSubmissionConnector)
    )
    .build()

  private lazy val service = app.injector.instanceOf[DmsSubmissionService]
  private lazy val applicationTemplate = app.injector.instanceOf[ApplicationPdf]
  private implicit lazy val mat: Materializer = app.injector.instanceOf[Materializer]

  "submitApplication" - {

    val trader = TraderDetail("eori", "name", "line1", None, None, "postcode", "GB", None)
    val goodsDetails = GoodsDetails("name", "description", None, None, None)
    val method = MethodOne(None, None, None)
    val contact = ContactDetails("name", "email", None)
    val now = Instant.now.truncatedTo(ChronoUnit.MILLIS)

    val attachment = Attachment(
      id = 1,
      name = "attachment",
      description = None,
      location = "some/location.pdf",
      privacy = Privacy.Public,
      mimeType = "application/pdf",
      size = 1337,
      contentMd5 = "contentMd5"
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
      created = now,
      lastUpdated = now
    )

    val hc: HeaderCarrier = HeaderCarrier()

    "must create a PDF from the application and send it to DMS Submission" in {

      val bytes = "Hello, World!".getBytes("UTF-8")
      val sourceCaptor: ArgumentCaptor[Source[ByteString, _]] = ArgumentCaptor.forClass(classOf[Source[ByteString, _]])

      when(mockFopService.render(any())).thenReturn(Future.successful(bytes))
      when(mockDmsSubmissionConnector.submitApplication(any(), any(), any(), any())(any())).thenReturn(Future.successful(Done))

      val expectedXml = applicationTemplate(application).body

      service.submitApplication(application)(hc).futureValue

      verify(mockFopService).render(eqTo(expectedXml))
      verify(mockDmsSubmissionConnector).submitApplication(eqTo("applicantEori"), sourceCaptor.capture(), eqTo(Seq(attachment)), eqTo(application.created))(eqTo(hc))

      val result = sourceCaptor.getValue().toMat(Sink.fold(ByteString.emptyByteString)(_ ++ _))(Keep.right).run().futureValue

      result.decodeString("UTF-8") mustEqual "Hello, World!"
    }

    "must fail if the fop service fails" in {

      when(mockFopService.render(any())).thenReturn(Future.failed(new RuntimeException()))

      service.submitApplication(application)(hc).failed.futureValue

      verify(mockDmsSubmissionConnector, never).submitApplication(any(), any(), any(), any())(any())
    }

    "must fail if the dms submission connector fails" in {

      when(mockFopService.render(any())).thenReturn(Future.successful(Array.emptyByteArray))
      when(mockDmsSubmissionConnector.submitApplication(any(), any(), any(), any())(any())).thenReturn(Future.failed(new RuntimeException()))

      service.submitApplication(application)(hc).failed.futureValue
    }
  }
}
