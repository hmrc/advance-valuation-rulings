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
import uk.gov.hmrc.advancevaluationrulings.views.xml.TraderInfo

class TraderInfoViewSpec extends BaseViewSpec {

  private val view: TraderInfo = app.injector.instanceOf[TraderInfo]

  val viewViaApply: Xml  = view.apply(application)
  val viewViaRender: Xml = view.render(application, messages)
  val viewViaF: Xml      = view.f(application)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.traderInfo"),
    messages("pdf.agentTraderEori"),
    messages("pdf.agentTrader.traderBusinessName"),
    messages("pdf.agentTrader.address"),
    s"""<fo:block margin-bottom="3mm">${application.trader.eori}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${application.trader.businessName}</fo:block>""",
    s"""<fo:block>${application.trader.addressLine1}</fo:block>""",
    s"""<fo:block>${application.trader.postcode}</fo:block>""",
    s"""<fo:block>${application.trader.country.name}</fo:block>"""
  )

  "TraderInfoView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
