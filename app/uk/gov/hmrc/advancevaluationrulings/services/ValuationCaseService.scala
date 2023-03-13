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

import cats.data.OptionT
import uk.gov.hmrc.advancevaluationrulings.models.{Attachment, CaseStatus, CaseWorker, RejectReason, ValuationApplication, ValuationCase}
import uk.gov.hmrc.advancevaluationrulings.repositories.ValuationCaseRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait ValuationCaseService {
  def assignCase(reference: String, caseWorker: CaseWorker): Future[Long]

  def unAssignCase(reference: String, caseWorker: CaseWorker): Future[Long]

  def findByReference(reference: String): Future[Option[ValuationCase]]

  def create(reference: String, valuation: ValuationApplication): Future[String]

  def allOpenCases: Future[List[ValuationCase]]

  def findByAssignee(assignee: String): Future[List[ValuationCase]]

  def rejectCase(reference: String, reason: RejectReason.Value, attachment: Attachment, note: String): Future[Long]
}

class MongoValuationCaseService @Inject() (repository: ValuationCaseRepository)(implicit ec: ExecutionContext) extends ValuationCaseService {
  def allOpenCases: Future[List[ValuationCase]] = repository.allOpenCases()

  override def create(reference: String, valuation: ValuationApplication): Future[String] = {
    val valuationCase = ValuationCase(reference,CaseStatus.NEW,Instant.now(),0, valuation,0)
    repository.create(valuationCase).map(_.toString)
  }

  override def findByReference(reference: String): Future[Option[ValuationCase]] =
    for{
      l <- repository.findByReference(reference)
    } yield l.headOption

  override def findByAssignee(assignee: String): Future[List[ValuationCase]] =
    for{
      l <- repository.findByAssignee(assignee)
    } yield l

  override def assignCase(reference: String, caseWorker: CaseWorker): Future[Long] = repository.assignCase(reference, caseWorker)

  override def rejectCase(reference: String, reason: RejectReason.Value, attachment: Attachment, note: String): Future[Long] = {
     def updateItem(item: ValuationCase) = item.copy(status = CaseStatus.REJECTED, attachments = item.attachments :+ attachment)
     val outcome = for{
       item <- OptionT(findByReference((reference)))
       count    <- OptionT.liftF(repository.replaceItem(updateItem(item)))
     } yield count

    outcome.getOrElse(throw new Exception("failed to update state to rejected"))
  }

  override def unAssignCase(reference: String, caseWorker: CaseWorker): Future[Long] = repository.unAssignCase(reference, caseWorker)

}
