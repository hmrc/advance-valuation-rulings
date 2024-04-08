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

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.i18n.{Messages, MessagesApi}
import uk.gov.hmrc.advancevaluationrulings.connectors.DmsSubmissionConnector
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.Application
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import java.io.IOException
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

abstract class DmsSubmissionService {

  def submitApplication(application: Application, submissionReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Done]
}

@Singleton
class SaveFileDmsSubmissionService @Inject() (
  fopService: FopService,
  pdfTemplate: ApplicationPdf,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext)
    extends DmsSubmissionService
    with Logging {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq.empty)

  override def submitApplication(application: Application, submissionReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    for {
      pdfBytes <- fopService.render(pdfTemplate(application).body)
    } yield writeFile(pdfBytes)

  private def writeFile(pdfBytes: Array[Byte]): Done = {
    val fileName = "applications/" + "application.pdf"
    try {
      Files.write(Paths.get(fileName), pdfBytes)
      Done
    } catch {
      case _: IOException =>
        logger.error(
          "[SaveFileDmsSubmissionService][writeFile] Failed to write local file of application"
        )
        Done
    }
  }
}

@Singleton
class DefaultDmsSubmissionService @Inject() (
  dmsConnector: DmsSubmissionConnector,
  fopService: FopService,
  pdfTemplate: ApplicationPdf,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext)
    extends DmsSubmissionService {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq.empty)

  override def submitApplication(application: Application, submissionReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Done] =
    for {
      pdfBytes <- fopService.render(pdfTemplate(application).body)
      _        <- submitToDms(application, pdfBytes, submissionReference)
    } yield Done

  private def submitToDms(
    application: Application,
    pdfBytes: Array[Byte],
    submissionReference: String
  )(implicit hc: HeaderCarrier): Future[Done] =
    dmsConnector.submitApplication(
      eori = application.applicantEori,
      pdf = Source.single(ByteString(pdfBytes)),
      timestamp = application.created,
      submissionReference = submissionReference,
      attachments = application.attachments,
      letterOfAuthority = application.letterOfAuthority
    )

}
