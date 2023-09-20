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

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

import play.api.{Configuration, Logging}
import play.api.http.Status.ACCEPTED
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.{Attachment, Privacy}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import cats.implicits._

import DmsSubmissionConnector._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import config.Service

@Singleton
class DmsSubmissionConnector @Inject() (
  configuration: Configuration,
  objectStoreClient: PlayObjectStoreClient,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends Logging {

  private val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  private val dmsSubmission: Service =
    configuration.get[Service]("microservice.services.dms-submission")

  private val dmsSubmissionConfig: Configuration =
    configuration.get[Configuration]("microservice.services.dms-submission")
  private val callbackUrl: String                = dmsSubmissionConfig.get[String]("callbackUrl")
  private val source: String                     = dmsSubmissionConfig.get[String]("source")
  private val formId: String                     = dmsSubmissionConfig.get[String]("formId")
  private val classificationType: String         = dmsSubmissionConfig.get[String]("classificationType")
  private val businessArea: String               = dmsSubmissionConfig.get[String]("businessArea")

  private def fileName(attachment: Attachment, name: String): String = attachment.privacy match {

    // TODO LOA logic
    case Privacy.Confidential => s"CONFIDENTIAL_$name"
    case _                    => name
  }

  def submitApplication(
    eori: String,
    pdf: Source[ByteString, _],
    timestamp: Instant,
    submissionReference: String,
    attachments: Seq[Attachment]
  )(implicit hc: HeaderCarrier): Future[Done] = {

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
      MultipartFormData.DataPart("metadata.businessArea", businessArea)
    )

    val fileParts = Seq(
      MultipartFormData.FilePart(
        key = "form",
        filename = "application.pdf",
        contentType = Some("application/pdf"),
        ref = pdf
      )
    )

    val attachmentParts = attachments.traverse {
      attachment =>
        objectStoreClient.getObject(Path.File(attachment.location)).flatMap {
          _.map {
            o =>
              Future.successful(
                MultipartFormData.FilePart(
                  key = "attachment",
                  filename = fileName(attachment, o.location.fileName),
                  contentType = Some(o.metadata.contentType),
                  ref = o.content
                )
              )
          }.getOrElse(Future.failed(AttachmentNotFoundException(attachment.location)))
        }
    }

    attachmentParts.flatMap {
      attachmentParts =>
        httpClient
          .post(url"$dmsSubmission/dms-submission/submit")
          .setHeader("Authorization" -> internalAuthToken)
          .withBody(
            Source(
              dataParts ++ fileParts ++ attachmentParts
            )
          )
          .execute[HttpResponse]
          .flatMap {
            response =>
              if (response.status == ACCEPTED) {
                Future.successful(Done)
              } else {
                logger.warn(s"dms-submission failed with response body: ${response.body}")
                Future.failed(
                  UpstreamErrorResponse(
                    "Unexpected response from dms-submission",
                    response.status,
                    reportAs = 500
                  )
                )
              }
          }
    }
  }
}

object DmsSubmissionConnector {

  final case class AttachmentNotFoundException(file: String) extends Exception with NoStackTrace {
    override def getMessage: String = super.getMessage
  }
}
