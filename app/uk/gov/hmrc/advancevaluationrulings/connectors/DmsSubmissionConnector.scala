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

package uk.gov.hmrc.advancevaluationrulings.connectors

import cats.implicits._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.mvc.MultipartFormData
import play.api.{Configuration, Logging}
import uk.gov.hmrc.advancevaluationrulings.config.Service
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.advancevaluationrulings.models.application.{Attachment, Privacy}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import play.api.libs.ws.bodyWritableOf_Multipart

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

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

  private def fileName(attachment: DmsAttachment, name: String): String =
    (attachment.privacy, attachment.isLetterOfAuthority) match {

      case (_, true)                 => "Letter_of_authority." + getExtension(name)
      case (Privacy.Confidential, _) => s"CONFIDENTIAL_$name"
      case _                         => name
    }

  private def getExtension(filename: String): String =
    if (filename == null || filename.trim.isEmpty) {
      logger.error("[DmsSubmissionConnector][getExtension] Invaild file extension")
      ""
      // "invalid" // Handle null or empty filename
    } else {
      val dotIndex = filename.lastIndexOf('.')
      if (dotIndex >= 0 && dotIndex < filename.length - 1) {
        filename.substring(dotIndex + 1).toLowerCase
      } else {
        logger.error("[DmsSubmissionConnector][getExtension] Invaild file extension")
        ""
      }
    }

  def submitApplication(
    eori: String,
    pdf: Source[ByteString, ?],
    timestamp: Instant,
    submissionReference: String,
    attachments: Seq[Attachment],
    letterOfAuthority: Option[Attachment]
  )(implicit hc: HeaderCarrier): Future[Done] = {

    val dateOfReceipt = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
      LocalDateTime.ofInstant(timestamp.truncatedTo(ChronoUnit.SECONDS), ZoneOffset.UTC)
    )

    val dmsAttachments = attachments.map(attachment =>
      DmsAttachment(
        attachment,
        isLetterOfAuthority = false
      )
    ) ++ letterOfAuthority.map(loa => DmsAttachment(loa, isLetterOfAuthority = true))

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

    val fileParts: Seq[MultipartFormData.FilePart[Source[ByteString, ?]]] =
      Seq(
        MultipartFormData.FilePart(
          key = "form",
          filename = "application.pdf",
          contentType = Some("application/pdf"),
          ref = pdf
        )
      )

    val attachmentParts = dmsAttachments.traverse { attachment =>
      objectStoreClient.getObject(Path.File(attachment.location)).flatMap {
        _.map { o =>
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

    attachmentParts.flatMap { attachmentParts =>
      httpClient
        .post(url"$dmsSubmission/dms-submission/submit")
        .setHeader("Authorization" -> internalAuthToken)
        .withBody(
          Source(
            dataParts ++ fileParts ++ attachmentParts
          )
        )
        .execute[HttpResponse]
        .flatMap { response =>
          if (response.status == ACCEPTED) {
            Future.successful(Done)
          } else {
            logger.error(
              s"[DmsSubmissionConnector][submitApplication] dms-submission failed with response body: ${response.body}"
            )
            Future.failed(
              UpstreamErrorResponse(
                "Unexpected response from dms-submission",
                response.status,
                reportAs = INTERNAL_SERVER_ERROR
              )
            )
          }
        }
    }
  }

}

case class DmsAttachment(
  id: Long,
  name: String,
  description: Option[String],
  location: String,
  privacy: Privacy,
  mimeType: String,
  size: Long,
  isLetterOfAuthority: Boolean
)

object DmsAttachment {
  def apply(attachment: Attachment, isLetterOfAuthority: Boolean): DmsAttachment =
    DmsAttachment(
      attachment.id,
      attachment.name,
      attachment.description,
      attachment.location,
      attachment.privacy,
      attachment.mimeType,
      attachment.size,
      isLetterOfAuthority
    )
}
