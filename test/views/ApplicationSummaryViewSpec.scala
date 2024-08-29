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
import uk.gov.hmrc.advancevaluationrulings.models.application.{Application, WhatIsYourRole}
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationSummary

import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern

class ApplicationSummaryViewSpec extends BaseViewSpec {

  private val view: ApplicationSummary = app.injector.instanceOf[ApplicationSummary]

  private val testApplication: Application = application.copy(
    agent = Some(traderDetail),
    whatIsYourRoleResponse = Some(WhatIsYourRole.AgentTrader)
  )

  val viewViaApply: Xml  = view.apply(testApplication)
  val viewViaRender: Xml = view.render(testApplication, messages)
  val viewViaF: Xml      = view.f(testApplication)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.applicationSummary"),
    messages("pdf.applicationId"),
    messages("pdf.submitted"),
    messages("pdf.traderInfo"),
    messages("pdf.agentTraderEori"),
    messages("pdf.agentTrader.traderBusinessName"),
    messages("pdf.agentTrader.address"),
    messages("pdf.agentDetail"),
    messages("pdf.agentTrader.name"),
    messages("pdf.agentTrader.email"),
    messages("pdf.agentTrader.phone"),
    messages("pdf.agentTrader.agentBusinessName"),
    messages("pdf.agentTrader.agentJobTitle"),
    """<fo:external-graphic src="url(conf/resources/logo.jpg)" padding-right="1cm" """ +
      """fox:alt-text="HM Revenue and Customs logo" />""",
    """<fo:block  margin-left="7.5cm" margin-top="-1.5cm" """ +
      s"""text-align="right" font-size="18pt" font-weight="bold">${messages("pdf.title")}""",
    """<fo:block font-size="11pt">""" +
      s"""${messages("pdf.page")} <fo:page-number/> ${messages("pdf.of")} """ +
      """<fo:page-number-citation ref-id="FinalElement" /></fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.id.toString}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${ofPattern("d MMMM yyyy")
      .withZone(ZoneId.systemDefault())
      .format(now)}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.trader.eori}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.trader.businessName}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.contact.name}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.contact.email}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.contact.phone.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.contact.companyName.get}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${testApplication.contact.jobTitle.get}</fo:block>""",
    s"""<fo:block>${testApplication.trader.addressLine1}</fo:block>""",
    s"""<fo:block>${testApplication.trader.postcode}</fo:block>""",
    s"""<fo:block>${testApplication.trader.country.name}</fo:block>"""
  )

  "ApplicationSummaryView" - {
    normalPage(expectedContent)
  }
}
