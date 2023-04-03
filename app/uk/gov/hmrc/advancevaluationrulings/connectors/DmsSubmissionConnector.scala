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

package uk.gov.hmrc.advancevaluationrulings.connectors

import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.Service
import play.api.Configuration
import play.api.http.Status.ACCEPTED
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.Attachment
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DmsSubmissionConnector @Inject() (
                                         configuration: Configuration,
                                         httpClient: HttpClientV2
                                       )(implicit ec: ExecutionContext) {

  private val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  private val dmsSubmission: Service = configuration.get[Service]("microservice.services.dms-submission")

  private val dmsSubmissionConfig: Configuration = configuration.get[Configuration]("microservice.services.dms-submission")
  private val callbackUrl: String = dmsSubmissionConfig.get[String]("callbackUrl")
  private val source: String = dmsSubmissionConfig.get[String]("source")
  private val formId: String = dmsSubmissionConfig.get[String]("formId")
  private val classificationType: String = dmsSubmissionConfig.get[String]("classificationType")
  private val businessArea: String = dmsSubmissionConfig.get[String]("businessArea")

  def submitApplication(eori: String, pdf: Source[ByteString, _], attachments: Seq[Attachment], timestamp: Instant, submissionReference: String)(implicit hc: HeaderCarrier): Future[Done] = {

    val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
      LocalDateTime.ofInstant(timestamp.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
    )

    val dataParts = Seq(
      MultipartFormData.DataPart("callbackUrl", callbackUrl),
      MultipartFormData.DataPart("submissionReference", submissionReference),
      MultipartFormData.DataPart("metadata.source", source),
      MultipartFormData.DataPart("metadata.timeOfReceipt", dateOfReceipt),
      MultipartFormData.DataPart("metadata.formId", formId),
      MultipartFormData.DataPart("metadata.customerId", eori),
      MultipartFormData.DataPart("metadata.classificationType", classificationType),
      MultipartFormData.DataPart("metadata.businessArea", businessArea),
    )

    val attachmentParts = attachments.zipWithIndex.flatMap { case (attachment, i) =>
      Seq(
        MultipartFormData.DataPart(s"attachments[$i].location", attachment.location),
        MultipartFormData.DataPart(s"attachments[$i].contentMd5", attachment.contentMd5)
      )
    }

    val fileParts = Seq(
      MultipartFormData.FilePart(
        key = "form",
        filename = "application.pdf",
        contentType = Some("application/pdf"),
        ref = pdf,
      )
    )

    httpClient.post(url"$dmsSubmission/dms-submission/submit")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(
        Source(
          dataParts ++ attachmentParts ++ fileParts
        )
      ).execute[HttpResponse].flatMap { response =>
        if (response.status == ACCEPTED) {
          Future.successful(Done)
        } else {
          Future.failed(UpstreamErrorResponse("Unexpected response from dms-submission", response.status, reportAs = 500))
        }
      }
  }
}
