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

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.MockitoSugar
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.Application
import play.api.inject.bind
import uk.gov.hmrc.advancevaluationrulings.base.SpecBase
import uk.gov.hmrc.advancevaluationrulings.connectors.DraftAttachmentsConnector
import uk.gov.hmrc.advancevaluationrulings.models.application.{ApplicationId, DraftAttachment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod.OneWeek
import uk.gov.hmrc.objectstore.client.config.ObjectStoreClientConfig
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.play.test.stub

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AttachmentsServiceSpec
    extends AnyFreeSpec
    with SpecBase
    with IntegrationPatience
    with MockitoSugar
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private implicit lazy val as: ActorSystem = ActorSystem()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAttachmentsConnector)
  }

  override def afterAll(): Unit = {
    as.terminate().futureValue
    super.afterAll()
  }

  private val baseUrl         = "baseUrl"
  private val owner           = "owner"
  private val token           = "token"
  private val config          = ObjectStoreClientConfig(baseUrl, owner, token, OneWeek)
  private val objectStoreStub = new stub.StubPlayObjectStoreClient(config)

  private val mockAttachmentsConnector = mock[DraftAttachmentsConnector]

  private lazy val app: Application = applicationBuilder
    .overrides(
      bind[PlayObjectStoreClient].toInstance(objectStoreStub),
      bind[DraftAttachmentsConnector].toInstance(mockAttachmentsConnector)
    )
    .build()

  private lazy val service: AttachmentsService = app.injector.instanceOf[AttachmentsService]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val fileContent        = "Hello, World"
  private val source             = Source.single(ByteString.fromString(fileContent))
  private val frontendAttachment =
    DraftAttachment(source, "application/pdf", "grtBN0au5C+J3qK1lhT57w==")
  private val applicationId      = ApplicationId(1337)

  "copyAttachment" - {

    "must fetch the attachment from the frontend and put it into object-store" in {

      val expectedPath = Path.File("attachments/GBAVR000001337/file.pdf")

      when(mockAttachmentsConnector.get(any())(any()))
        .thenReturn(Future.successful(frontendAttachment))

      service
        .copyAttachment(applicationId, "path/to/file.pdf")(hc)
        .futureValue mustEqual expectedPath

      verify(mockAttachmentsConnector).get(eqTo("path/to/file.pdf"))(eqTo(hc))

      val savedObject = objectStoreStub.getObject(expectedPath).futureValue.value
      savedObject.metadata.contentLength mustEqual ByteString.fromString(fileContent).length
      savedObject.metadata.contentType mustEqual "application/pdf"
      savedObject.metadata.contentMd5.value mustEqual "grtBN0au5C+J3qK1lhT57w=="

      val result =
        savedObject.content.runWith(Sink.reduce[ByteString](_ ++ _)).futureValue.utf8String
      result mustEqual fileContent
    }

    "must fail if the connector fails" in {

      when(mockAttachmentsConnector.get(any())(any()))
        .thenReturn(Future.failed(new RuntimeException()))

      service.copyAttachment(applicationId, "path/to/file.pdf")(hc).failed.futureValue
    }
  }
}
