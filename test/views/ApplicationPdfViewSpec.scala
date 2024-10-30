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
import uk.gov.hmrc.advancevaluationrulings.views.xml.ApplicationPdf

import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern

class ApplicationPdfViewSpec extends BaseViewSpec {

  private val view: ApplicationPdf = app.injector.instanceOf[ApplicationPdf]

  val viewViaApply: Xml  = view.apply(application)
  val viewViaRender: Xml = view.render(application, messages)
  val viewViaF: Xml      = view.f(application)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.applicationSummary"),
    messages("pdf.traderEori"),
    messages("pdf.applicationId"),
    messages("pdf.submitted"),
    messages("pdf.traderDetail"),
    messages("pdf.trader.businessName"),
    messages("pdf.trader.address"),
    messages("pdf.applicantName"),
    messages("pdf.applicantEmail"),
    messages("pdf.applicantPhone"),
    messages("pdf.agent.businessName"),
    messages("pdf.applicantJobTitle"),
    messages("pdf.whatIsYourRole.label"),
    """<fo:layout-master-set>""",
    """</fo:layout-master-set>""",
    """<fo:region-before region-name="xsl-region-before"/>""",
    """<fo:region-after region-name="xsl-region-after"/>""",
    """<fo:bookmark internal-destination="title">""",
    s"""<fo:bookmark-title>${messages("pdf.title")}</fo:bookmark-title>""",
    """<fo:external-graphic src="url(conf/resources/logo.jpg)" padding-right="1cm" """ +
      """fox:alt-text="HM Revenue and Customs logo" />""",
    """<fo:block  margin-left="7.5cm" margin-top="-1.5cm" """ +
      s"""text-align="right" font-size="18pt" font-weight="bold">${messages("pdf.title")}""",
    """<fo:block font-size="11pt">""" +
      s"""${messages("pdf.page")} <fo:page-number/> ${messages("pdf.of")} """ +
      """<fo:page-number-citation ref-id="FinalElement" /></fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.trader.eori}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.id.toString}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${ofPattern("d MMMM yyyy")
        .withZone(ZoneId.systemDefault())
        .format(now)}</fo:block>""",
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

  "ApplicationPdfView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
