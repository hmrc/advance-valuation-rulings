/*
 * Copyright 2025 HM Revenue & Customs
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

package views

import play.twirl.api.Xml
import uk.gov.hmrc.advancevaluationrulings.views.xml.DescribeRoleForEmployee

class DescribeRoleForEmployeeViewSpec extends BaseViewSpec {

  private val view: DescribeRoleForEmployee = app.injector.instanceOf[DescribeRoleForEmployee]

  val viewViaApply: Xml  = view.apply(employeeOrgRole)
  val viewViaRender: Xml = view.render(employeeOrgRole, messages)
  val viewViaF: Xml      = view.f(employeeOrgRole)(messages)

  private val employeeOrgExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.employeeOrg")}</fo:block>"""
  )

  private val agentTraderExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.agentTrader")}</fo:block>"""
  )

  private val otherExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.unansweredLegacySupport")}</fo:block>"""
  )

  "DescribeRoleForEmployeeView" - {
    normalPage(employeeOrgExpectedContent)

    Seq(
      (agentOrgRole, Seq.empty),
      (agentTraderRole, agentTraderExpectedContent),
      (otherRole, otherExpectedContent)
    ).foreach { case (role, expectedContent) =>
      s"must display correct content for role $role" - {
        val renderedView: Xml = view.apply(role)
        checkRenderedContent(renderedView, expectedContent, ".apply")
      }
    }

    view.ref must not be None.orNull
  }
}
