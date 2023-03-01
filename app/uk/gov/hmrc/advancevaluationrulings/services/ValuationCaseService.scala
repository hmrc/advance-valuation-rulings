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

import uk.gov.hmrc.advancevaluationrulings.models.{CaseStatus, CaseWorker, ValuationApplication, ValuationCase}
import uk.gov.hmrc.advancevaluationrulings.repositories.ValuationCaseRepository

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait ValuationCaseService {
  def assignCase(reference: String, caseWorker: CaseWorker): Future[Long]

  def findByReference(reference: String): Future[Option[ValuationCase]]

  def create(reference: String, valuation: ValuationApplication): Future[String]

  def allOpenCases: Future[List[ValuationCase]]
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

  override def assignCase(reference: String, caseWorker: CaseWorker): Future[Long] = repository.assignCase(reference, caseWorker)
}
