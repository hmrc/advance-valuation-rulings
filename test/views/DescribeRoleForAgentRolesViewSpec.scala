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

package views

import play.twirl.api.Xml
import uk.gov.hmrc.advancevaluationrulings.views.xml.DescribeRoleForAgentRoles

class DescribeRoleForAgentRolesViewSpec extends BaseViewSpec {

  private val view: DescribeRoleForAgentRoles = app.injector.instanceOf[DescribeRoleForAgentRoles]

  val viewViaApply: Xml  = view.apply(agentOrgRole)
  val viewViaRender: Xml = view.render(agentOrgRole, messages)
  val viewViaF: Xml      = view.f(agentOrgRole)(messages)

  private val agentOrgExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.agentOrg")}</fo:block>"""
  )

  private val agentTraderExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.agentTrader")}</fo:block>"""
  )

  private val otherExpectedContent: Seq[String] = Seq(
    s"""${messages("pdf.whatIsYourRole.label")}""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.unansweredLegacySupport")}</fo:block>"""
  )

  "DescribeRoleForAgentRolesView" - {
    normalPage(agentOrgExpectedContent)

    Seq(
      (agentTraderRole, agentTraderExpectedContent),
      (employeeOrgRole, Seq.empty),
      (otherRole, otherExpectedContent)
    ).foreach { case (role, expectedContent) =>
      s"must display correct content for role $role" - {
        val renderedView: Xml = view.apply(role)
        checkRenderedContent(renderedView, expectedContent)
      }
    }
  }
}
