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
import uk.gov.hmrc.advancevaluationrulings.views.xml.TraderDetail

class TraderDetailViewSpec extends BaseViewSpec {

  private val view: TraderDetail = app.injector.instanceOf[TraderDetail]

  val viewViaApply: Xml  = view.apply(application)
  val viewViaRender: Xml = view.render(application, messages)
  val viewViaF: Xml      = view.f(application)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.traderDetail"),
    messages("pdf.trader.businessName"),
    messages("pdf.trader.address"),
    messages("pdf.applicantName"),
    messages("pdf.applicantEmail"),
    messages("pdf.applicantPhone"),
    messages("pdf.agent.businessName"),
    messages("pdf.applicantJobTitle"),
    messages("pdf.whatIsYourRole.label"),
    s"""<fo:block margin-bottom="3mm">${application.trader.businessName}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.name}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.email}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.phone.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.companyName.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.contact.jobTitle.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.whatIsYourRole.employeeOrg")}</fo:block>""",
    s"""<fo:block>${application.trader.addressLine1}</fo:block>""",
    s"""<fo:block>${application.trader.postcode}</fo:block>""",
    s"""<fo:block>${application.trader.country.name}</fo:block>"""
  )

  "TraderDetailView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
