/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.bindingtariffclassification.model.reporting

import java.net.URLDecoder
import java.time.Instant

import play.api.mvc.QueryStringBindable.bindableString
import uk.gov.hmrc.bindingtariffclassification.model.reporting.CaseReportFilter.binder
import uk.gov.hmrc.bindingtariffclassification.model.{ApplicationType, CaseStatus}
import uk.gov.hmrc.play.test.UnitSpec

class CaseReportTest extends UnitSpec {

  private val report = CaseReport(
    filter = CaseReportFilter(
      decisionStartDate = Some(InstantRange(
        min = Instant.EPOCH,
        max = Instant.EPOCH.plusSeconds(1)
      )),
      referralDate = Some(InstantRange(
        min = Instant.EPOCH,
        max = Instant.EPOCH.plusSeconds(1)
      )),
      reference = Some(Set("reference1", "reference2")),
      status = Some(Set(CaseStatus.OPEN, CaseStatus.NEW)),
      applicationType = Some(Set(ApplicationType.BTI, ApplicationType.LIABILITY_ORDER)),
      assigneeId = Some("assignee_123")
    ),
    field = CaseReportField.ACTIVE_DAYS_ELAPSED,
    group = Set(CaseReportGroup.QUEUE)
  )

  private val params: Map[String, Seq[String]] = Map(
    "min_decision_start" -> Seq("1970-01-01T00:00:00Z"),
    "max_decision_start" -> Seq("1970-01-01T00:00:01Z"),
    "min_referral_date" -> Seq("1970-01-01T00:00:00Z"),
    "max_referral_date" -> Seq("1970-01-01T00:00:01Z"),
    "report_field" -> Seq("active-days-elapsed"),
    "report_group" -> Seq("queue-id"),
    "reference" -> Seq("reference1,reference2"),
    "status" -> Seq("OPEN", "NEW"),
    "application_type" -> Seq("BTI", "LIABILITY_ORDER"),
    "assignee_id" -> Seq("assignee_123")
  )

  /**
    * When we add fields to Report these tests shouldn't need changing, only the fields above.
    **/
  "Report Binder" should {

    "Unbind Populated Search to Query String" in {
      val populatedQueryParam: String =
        "min_decision_start=1970-01-01T00:00:00Z&" +
          "max_decision_start=1970-01-01T00:00:01Z&" +
          "min_referral_date=1970-01-01T00:00:00Z&" +
          "max_referral_date=1970-01-01T00:00:01Z&" +
          "reference=reference1&" +
          "reference=reference2&" +
          "status=OPEN&" +
          "status=NEW&" +
          "application_type=BTI&" +
          "application_type=LIABILITY_ORDER&" +
          "assignee_id=assignee_123&" +
          "report_group=queue-id&" +
          "report_field=active-days-elapsed"

      URLDecoder.decode(CaseReport.bindable.unbind("", report), "UTF-8") shouldBe populatedQueryParam
    }

    "Bind empty query string" in {
      CaseReport.bindable.bind("", Map()) shouldBe Some(Left("Invalid Field/Group"))
    }

    "Bind query string with empty field" in {
      CaseReport.bindable.bind("", params.filterNot(_._1 == "report_field")) shouldBe Some(Left("Invalid Field"))
    }

    "Bind query string with empty group" in {
      CaseReport.bindable.bind("", params.filterNot(_._1 == "report_group")) shouldBe Some(Left("Invalid Group"))
    }

    "Bind populated query string" in {
      CaseReport.bindable.bind("", params) shouldBe Some(Right(report))
    }

  }

}
