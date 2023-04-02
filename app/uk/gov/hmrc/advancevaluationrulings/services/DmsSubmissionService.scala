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

import akka.stream.scaladsl.Source
import akka.util.ByteString
import uk.gov.hmrc.advancevaluationrulings.connectors.DmsSubmissionConnector
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.Application
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsSubmissionService @Inject() (
                                       dmsConnector: DmsSubmissionConnector,
                                       fopService: FopService,
                                       pdfTemplate: ApplicationPdf
                                     )(implicit ec: ExecutionContext) {

  def submitApplication(application: Application)(implicit hc: HeaderCarrier): Future[Done] =
    for {
      pdfBytes <- fopService.render(pdfTemplate(application).body)
      _        <- submitToDms(application, pdfBytes)
    } yield Done

  private def submitToDms(application: Application, pdfBytes: Array[Byte])(implicit hc: HeaderCarrier): Future[Done] =
    dmsConnector.submitApplication(
      eori = application.applicantEori,
      pdf = Source.single(ByteString(pdfBytes)),
      attachments = application.attachments,
      timestamp = application.created
    )
}
