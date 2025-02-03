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
import uk.gov.hmrc.advancevaluationrulings.views.xml.AgentInfo

class AgentInfoViewSpec extends BaseViewSpec {

  private val view: AgentInfo = app.injector.instanceOf[AgentInfo]

  val viewViaApply: Xml  = view.apply(application)
  val viewViaRender: Xml = view.render(application, messages)
  val viewViaF: Xml      = view.f(application)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.agentDetail"),
    messages("pdf.agentTrader.name"),
    messages("pdf.agentTrader.email"),
    messages("pdf.agentTrader.phone"),
    messages("pdf.agentTrader.agentBusinessName"),
    messages("pdf.agentTrader.agentJobTitle"),
    s"""<fo:block margin-bottom="3mm">${application.contact.name}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.email}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.phone.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.companyName.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.jobTitle.get}</fo:block>"""
  )

  "AgentInfoView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
