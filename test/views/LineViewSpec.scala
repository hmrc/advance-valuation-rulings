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
import uk.gov.hmrc.advancevaluationrulings.views.xml.Line

class LineViewSpec extends BaseViewSpec {

  private val view: Line = app.injector.instanceOf[Line]

  private val messagesKey: String = "pdf.agent.address"

  val viewViaApply: Xml  = view.apply(messagesKey, addressLines)
  val viewViaRender: Xml = view.render(messagesKey, addressLines, messages)
  val viewViaF: Xml      = view.f(messagesKey, addressLines)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages(messagesKey),
    s"""<fo:block margin-bottom="3mm">${traderDetail.addressLine1}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${traderDetail.postcode}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${traderDetail.country.name}</fo:block>"""
  )

  "LineView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
