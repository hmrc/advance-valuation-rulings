/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.service

import javax.inject.Inject
import uk.gov.hmrc.bindingtariffclassification.model.CaseStatus.CaseStatus
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.model.reporting.v2._
import uk.gov.hmrc.bindingtariffclassification.model.reporting.{CaseReport => OldReport, InstantRange, ReportResult}
import uk.gov.hmrc.bindingtariffclassification.repository.{CaseRepository, EventRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.JsObject

class ReportService @Inject() (caseRepository: CaseRepository, eventRepository: EventRepository) {

  def summaryReport(report: SummaryReport, pagination: Pagination): Future[Paged[ResultGroup]] =
    caseRepository.summaryReport(report, pagination)

  def caseReport(report: CaseReport, pagination: Pagination): Future[Paged[Map[String, ReportResultField[_]]]] =
    caseRepository.caseReport(report, pagination: Pagination)

  def generate(report: OldReport): Future[Seq[ReportResult]] =
    for {
      report: OldReport <- report.filter.referralDate match {
                             case Some(range) => appendReferredCaseReferencesTo(report, range)
                             case None        => Future.successful(report)
                           }

      report <- caseRepository.generateReport(report)
    } yield report

  private def appendReferredCaseReferencesTo(report: OldReport, range: InstantRange): Future[OldReport] = {
    val caseStatusChangeEventTypes =
      Set(EventType.CASE_STATUS_CHANGE, EventType.CASE_REFERRAL, EventType.CASE_REJECTED, EventType.CASE_COMPLETED, EventType.CASE_CANCELLATION)
    val filter = report.filter
    val search = EventSearch(
      timestampMin = Some(range.min),
      timestampMax = Some(range.max),
      `type`       = Some(caseStatusChangeEventTypes)
    )

    eventRepository
      .search(search, Pagination.max)
      .map(_.results)
      .map {
        _.filter { event =>
          val change = event.details.asInstanceOf[FieldChange[CaseStatus]]
          change.to == CaseStatus.REFERRED || change.from == CaseStatus.REFERRED
        }
      }
      .map(_.map(_.caseReference))
      .map { referredCaseReferences =>
        val references = filter.reference.getOrElse(Set.empty) ++ referredCaseReferences
        report.copy(filter = filter.copy(reference = Some(references), referralDate = None))
      }
  }

}
