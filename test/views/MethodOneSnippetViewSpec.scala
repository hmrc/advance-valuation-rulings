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
import uk.gov.hmrc.advancevaluationrulings.views.xml.MethodOneSnippet

class MethodOneSnippetViewSpec extends BaseViewSpec {

  private val view: MethodOneSnippet = app.injector.instanceOf[MethodOneSnippet]

  val viewViaApply: Xml  = view.apply(methodOne)
  val viewViaRender: Xml = view.render(methodOne, messages)
  val viewViaF: Xml      = view.f(methodOne)(messages)

  private val expectedContent: Seq[String] = Seq(
    messages("pdf.method"),
    messages("pdf.saleInvolved"),
    messages("pdf.saleBetweenRelatedParties"),
    messages("pdf.saleBetweenRelatedPartiesDescription"),
    messages("pdf.goodsRestrictions"),
    messages("pdf.goodsRestrictionsDescription"),
    messages("pdf.saleConditions"),
    messages("pdf.saleConditionsDescription"),
    s"""<fo:block margin-bottom="3mm">${messages("pdf.method.1")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">${messages("pdf.yes")}</fo:block>""",
    s"""<fo:block margin-bottom="3mm">$randomString</fo:block>"""
  )

  "MethodOneSnippetView" - {
    normalPage(expectedContent)
    view.ref must not be None.orNull
  }
}
