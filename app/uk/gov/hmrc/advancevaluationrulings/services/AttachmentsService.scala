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
import cats.implicits._
import uk.gov.hmrc.advancevaluationrulings.connectors.{DraftAttachment, DraftAttachmentsConnector}
import uk.gov.hmrc.advancevaluationrulings.models.Done
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.objectstore.client.play.Implicits._
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Md5Hash, Path}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsService @Inject() (
                                     draftAttachmentsConnector: DraftAttachmentsConnector,
                                     objectStoreClient: PlayObjectStoreClient
                                   )(implicit ec: ExecutionContext, mat: Materializer) {

  def copyAttachment(applicationId: String, path: String)(implicit hc: HeaderCarrier): Future[Done] =
    for {
      attachment <- draftAttachmentsConnector.get(path)
      _          <- putAttachment(applicationId, path, attachment)
    } yield Done

  private def putAttachment(applicationId: String, path: String, attachment: DraftAttachment)(implicit hc: HeaderCarrier): Future[Done] =
    objectStoreClient.putObject(
      path = Path.Directory(s"attachments/$applicationId").file(Path.File(path).fileName),
      content = attachment.content,
      contentType = Some(attachment.contentType),
      contentMd5 = Some(Md5Hash(attachment.contentMd5))
    ).as(Done)
}
