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

import uk.gov.hmrc.advancevaluationrulings.models.application._
import uk.gov.hmrc.advancevaluationrulings.repositories.{ApplicationRepository, CounterRepository}

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplicationService @Inject()(
                                    counterRepository: CounterRepository,
                                    applicationRepository: ApplicationRepository,
                                    clock: Clock
                                  )(implicit ec: ExecutionContext) {

  def save(applicantEori: String, request: ApplicationRequest): Future[ApplicationId] =
    counterRepository.nextId(CounterId.ApplicationId).flatMap { appId =>
      buildAttachments(request.attachments).flatMap { attachments =>
        val applicationId = ApplicationId(appId)
        val application = Application(
          id = applicationId,
          applicantEori = applicantEori,
          trader = request.trader,
          agent = request.agent,
          contact = request.contact,
          goodsDetails = request.goodsDetails,
          requestedMethod = request.requestedMethod,
          attachments = attachments,
          created = Instant.now(clock),
          lastUpdated = Instant.now(clock)
        )

        applicationRepository
          .set(application)
          .map(_ => applicationId)
      }
    }

  private def buildAttachments(requests: Seq[AttachmentRequest]): Future[Seq[Attachment]] =
    Future.sequence(
      requests.map { request =>
        counterRepository.nextId(CounterId.AttachmentId).map { id =>
          Attachment(
            id = id,
            name = request.name,
            location = request.url,
            public = request.public,
            mimeType = request.mimeType,
            size = request.size,
            contentMd5 = request.contentMd5
          )
        }
      })
}
