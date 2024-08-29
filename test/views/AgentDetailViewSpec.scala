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
import uk.gov.hmrc.advancevaluationrulings.views.xml.AgentDetail

class AgentDetailViewSpec extends BaseViewSpec {

  private val view: AgentDetail = app.injector.instanceOf[AgentDetail]

  val viewViaApply: Xml  = view.apply(application, traderDetail)
  val viewViaRender: Xml = view.render(application, traderDetail, messages)
  val viewViaF: Xml      = view.f(application, traderDetail)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.agentDetail"),
    messages("pdf.agent.eori"),
    messages("pdf.agent.businessName"),
    messages("pdf.agent.address"),
    messages("pdf.agentName"),
    messages("pdf.agentEmail"),
    messages("pdf.agentPhone"),
    messages("pdf.agentJobTitle"),
    s"""<fo:block margin-bottom="3mm">${traderDetail.eori}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${traderDetail.businessName}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.name}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.email}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.phone.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.jobTitle.get}</fo:block>""",
    s"""<fo:block>${traderDetail.addressLine1}</fo:block>""",
    s"""<fo:block>${traderDetail.postcode}</fo:block>""",
    s"""<fo:block>${traderDetail.country.name}</fo:block>"""
  )

  "AgentDetailView" - {
    normalPage(expectedContent)
  }
}
